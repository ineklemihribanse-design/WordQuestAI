PATH="web/arena_engine.html"
CSP="default-src 'none'; script-src 'unsafe-inline'; style-src 'none'; img-src 'none'; connect-src 'none'; frame-src 'none'; object-src 'none'; base-uri 'none'; form-action 'none'"
def validate(data):
    assert 200<=len(data)<=128*1024
    s=data.decode("utf-8")
    assert s.startswith("<!DOCTYPE html>") and CSP in s and "window.wqGenerate" in s
    return s
