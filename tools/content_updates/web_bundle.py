PATH="web/arena_lobby.html"
MAX_BYTES=96*1024
CSP="default-src 'none'; script-src 'unsafe-inline'; style-src 'unsafe-inline'; img-src data:; connect-src 'none'; frame-src 'none'; object-src 'none'; base-uri 'none'; form-action 'none'"
def validate(data):
    assert 200<=len(data)<=MAX_BYTES
    s=data.decode("utf-8")
    assert s.startswith("<!DOCTYPE html>") and CSP in s and "window.wqSetState" in s
    return s
