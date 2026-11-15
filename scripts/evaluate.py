"""Run after ingestion: python3 scripts/evaluate.py > evaluation/local-result.json"""
import json, time, urllib.request, statistics, platform
cases=[json.loads(line) for line in open('evaluation/questions.jsonl')]
results=[]
for case in cases:
    start=time.perf_counter()
    try:
        request=urllib.request.Request('http://localhost:8080/api/questions',data=json.dumps({'question':case['question'],'maxSources':5}).encode(),headers={'Content-Type':'application/json'})
        with urllib.request.urlopen(request,timeout=90) as response: data=json.load(response)
        citations=data.get('citations',[])
        results.append({'question':case['question'],'outcomeCorrect':data['outcome']==case['expectedOutcome'],'hit':bool(set(case['relevantSourceKeys']) & {c['sourceKey'] for c in citations}),'citationMetadataPresent':all(c.get('chunkId') and c.get('url','').startswith('https://www.gov.uk/') for c in citations),'latencySeconds':time.perf_counter()-start})
    except Exception as e: results.append({'question':case['question'],'failure':type(e).__name__})
times=sorted(r['latencySeconds'] for r in results if 'latencySeconds' in r)
print(json.dumps({'environment':platform.platform(),'modelConfiguration':'Record model digests and index timestamp separately','volume':len(results),'failures':sum('failure' in r for r in results),'p50':statistics.median(times) if times else None,'p95':times[min(len(times)-1,int(len(times)*.95))] if times else None,'results':results,'limitation':'Metadata presence is not independent chunk resolution or semantic faithfulness.'},indent=2))
