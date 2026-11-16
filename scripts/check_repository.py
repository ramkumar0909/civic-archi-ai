"""Basic tracked-file checks; supplements, does not replace, a secret scanner."""
import pathlib, re, subprocess
for name in ['LICENSE', 'NOTICE.md', 'SECURITY.md']:
    assert pathlib.Path(name).is_file(), name
for name in subprocess.check_output(['git', 'ls-files', '--cached', '--others', '--exclude-standard'], text=True).splitlines():
    p = pathlib.Path(name)
    if not p.is_file():
        continue
    text = p.read_text(errors='replace')
    assert not re.search(r'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----', text), name
    assert not re.search(r'AKIA[0-9A-Z]{16}', text), name
print('Basic repository checks passed (tracked and untracked nonignored files)')
