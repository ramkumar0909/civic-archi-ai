package com.newroniq.civicarch.infrastructure;

import com.newroniq.civicarch.domain.Evidence;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
public class EvidenceStore
    implements com.newroniq.civicarch.domain.Retriever,
        com.newroniq.civicarch.domain.RevisionRepository {

  private final JdbcTemplate db;

  private final TransactionTemplate tx;

  public EvidenceStore(JdbcTemplate db, PlatformTransactionManager manager) {
    this.db = db;
    this.tx = new TransactionTemplate(manager);
  }

  static UUID id(String text) {
    return UUID.nameUUIDFromBytes(text.getBytes(StandardCharsets.UTF_8));
  }

  public static String vector(float[] values) {
    return Arrays.toString(values);
  }

  public boolean unchanged(String key, String hash) {
    return Boolean.TRUE.equals(
        db.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM source_document s JOIN document_revision r ON r.id=s.current_revision_id WHERE s.source_key=? AND r.revision_hash=?)",
            Boolean.class,
            key,
            hash));
  }

  public void refresh(String key, com.newroniq.civicarch.domain.PublishedContent content) {
    db.update(
        "UPDATE source_document SET retrieved_at=now(),last_attempt_at=now(),last_success_at=now(),status='ACTIVE',last_error_summary=NULL,title=?,public_updated_at=?::timestamptz,payload_sha256=? WHERE source_key=?",
        content.title(),
        content.updatedAt(),
        content.payloadHash(),
        key);
  }

  public void state(
      com.newroniq.civicarch.domain.SourceRegistry.Source source, String status, String error) {
    db.update(
        "INSERT INTO source_document(id,source_key,base_path,canonical_url,status,last_error_summary,last_attempt_at) VALUES(?,?,?,?,?,?,now()) ON CONFLICT(source_key) DO UPDATE SET status=excluded.status,last_error_summary=excluded.last_error_summary,last_attempt_at=now()",
        id(source.sourceKey()),
        source.sourceKey(),
        source.path(),
        "https://www.gov.uk" + source.path(),
        status,
        error);
  }

  public void publish(
      com.newroniq.civicarch.domain.SourceRegistry.Source source,
      com.newroniq.civicarch.domain.PublishedContent payload,
      String payloadHash,
      String revisionHash,
      List<com.newroniq.civicarch.domain.PublishedContent.Section> chunks,
      List<float[]> vectors,
      String model) {
    tx.executeWithoutResult(
        _ -> {
          UUID sid = id(source.sourceKey());
          UUID rid = id(source.sourceKey() + revisionHash);
          state(source, "ERROR", "Index publication in progress");
          db.queryForObject(
              "SELECT source_key FROM source_document WHERE id=? FOR UPDATE", String.class, sid);
          db.update(
              "INSERT INTO document_revision(id,source_document_id,revision_hash,extracted_text) VALUES(?,?,?,?) ON CONFLICT DO NOTHING",
              rid,
              sid,
              revisionHash,
              chunks.toString());
          for (int i = 0; i < chunks.size(); i++) {
            var c = chunks.get(i);
            db.update(
                "INSERT INTO document_chunk(id,revision_id,ordinal,heading_path,text,token_estimate,content_sha256,embedding_model_id,embedding_vector) VALUES(?,?,?,?,?,?,?,?,?::vector) ON CONFLICT DO NOTHING",
                id(rid + ":" + i),
                rid,
                i,
                c.heading(),
                c.text(),
                (c.text().length() + 3) / 4,
                ContentExtractor.hash(c.text()),
                model,
                vector(vectors.get(i)));
          }
          db.update(
              "UPDATE source_document SET content_id=?,title=?,document_type=?,locale=?,public_updated_at=?::timestamptz,retrieved_at=now(),last_attempt_at=now(),status='ACTIVE',payload_sha256=?,content_sha256=?,extraction_version='1',current_revision_id=?,last_success_at=now(),last_error_summary=NULL WHERE id=?",
              payload.contentId(),
              payload.title(),
              payload.documentType(),
              payload.locale(),
              payload.updatedAt(),
              payloadHash,
              revisionHash,
              rid,
              sid);
        });
  }

  public List<Evidence> retrieve(
      float[] query, String model, List<String> keys, int limit, double floor) {
    if (keys.isEmpty()) {
      return List.of();
    }
    if (Boolean.TRUE.equals(
        db.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM document_chunk c JOIN source_document s ON s.current_revision_id=c.revision_id WHERE s.status IN ('ACTIVE','ERROR') AND c.embedding_model_id<>?)",
            Boolean.class,
            model))) {
      throw new IllegalStateException("Embedding identity mismatch; complete reindex required");
    }
    return db.query(
        connection -> {
          var statement =
              connection.prepareStatement(
                  "SELECT c.id,s.source_key,s.title,s.canonical_url,s.content_id,s.public_updated_at,s.retrieved_at,c.heading_path,c.text,s.status,1-(c.embedding_vector <=> ?::vector) similarity FROM document_chunk c JOIN source_document s ON s.current_revision_id=c.revision_id WHERE s.status IN ('ACTIVE','ERROR') AND c.embedding_model_id=? AND s.source_key = ANY (?::text[]) AND 1-(c.embedding_vector <=> ?::vector)>=? ORDER BY c.embedding_vector <=> ?::vector,c.id LIMIT ?");
          statement.setString(1, vector(query));
          statement.setString(2, model);
          statement.setObject(3, keys.toArray(String[]::new));
          statement.setString(4, vector(query));
          statement.setDouble(5, floor);
          statement.setString(6, vector(query));
          statement.setInt(7, limit);
          return statement;
        },
        (rs, _) ->
            new Evidence(
                rs.getObject(1, UUID.class),
                rs.getString(2),
                rs.getString(3),
                rs.getString(4),
                rs.getString(5),
                rs.getTimestamp(6) == null ? null : rs.getTimestamp(6).toInstant(),
                rs.getTimestamp(7).toInstant(),
                rs.getString(8),
                rs.getString(9),
                rs.getString(10),
                rs.getDouble(11)));
  }

  public UUID startRun() {
    UUID id = UUID.randomUUID();
    db.update("INSERT INTO ingestion_run(id,status) VALUES(?,'RUNNING')", id);
    return id;
  }

  public void finishRun(
      UUID id, int attempted, int changed, int unchanged, int skipped, int failed) {
    db.update(
        "UPDATE ingestion_run SET completed_at=now(),status=?,attempted=?,changed=?,unchanged=?,skipped=?,failed=? WHERE id=?",
        failed == 0 ? "COMPLETE" : "PARTIAL",
        attempted,
        changed,
        unchanged,
        skipped,
        failed,
        id);
  }
}
