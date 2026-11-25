import base64,hashlib,json,subprocess,tempfile,unittest
from pathlib import Path
from make_content import make

class ContentTest(unittest.TestCase):
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root=Path(self.tmp.name)
        self.source=self.root/'input'
        self.source.mkdir()
        self.out=self.root/'output'
        self.key=self.root/'key.pem'
        self.pub=self.root/'pub.pem'
        subprocess.run(['openssl','genpkey','-algorithm','RSA','-pkeyopt','rsa_keygen_bits:2048','-out',str(self.key)],check=True,capture_output=True)
        subprocess.run(['openssl','pkey','-in',str(self.key),'-pubout','-out',str(self.pub)],check=True,capture_output=True)
    def test_signature_and_digest(self):
        p=self.source/'arena_v122'/'settings.webp'
        p.parent.mkdir()
        # Packer validates the container header; Android validates dimensions too.
        data=b'RIFF'+bytes(4)+b'WEBP'+b'fixture'
        p.write_bytes(data)
        make(self.source,self.out,150,'1.0.125',1,self.key)
        env=json.loads((self.out/'index.json').read_text())
        payload=base64.b64decode(env['payload'])
        sig=self.root/'sig'
        sig.write_bytes(base64.b64decode(env['signature']))
        verify=['openssl','dgst','-sha256','-verify',str(self.pub),'-signature',str(sig)]
        self.assertEqual(subprocess.run(verify,input=payload,capture_output=True).returncode,0)
        self.assertNotEqual(subprocess.run(verify,input=payload+b' ',capture_output=True).returncode,0)
        manifest=json.loads(payload)
        entry=manifest['files'][0]
        digest=hashlib.sha256(data).hexdigest()
        self.assertEqual(entry['sha256'],digest)
        self.assertEqual((self.out/'objects'/f'{digest}.webp').read_bytes(),data)
        self.assertEqual(manifest['coreCode'],150)
    def test_design_snapshot_and_rejection(self):
        from lobby_design import validate
        p=self.source/'design'/'arena_lobby.json'
        p.parent.mkdir()
        good=Path(__file__).resolve().parents[1]/'v128_lobby'/'arena_lobby.json'
        data=good.read_bytes()
        o=validate(data)
        p.write_bytes(data)
        make(self.source,self.out,153,'1.0.129',1,self.key)
        self.assertEqual((self.out/'objects'/(hashlib.sha256(data).hexdigest()+'.json')).read_bytes(),data)
        for patch in ({'actionHeight':0},{'phoneCardPercent':100},{'palette':'file://bad'},
                      {'sectionOrder':'javascript'},{'script':'run()'},{'nameSp':True}):
            p.write_text(json.dumps(dict(o,**patch)))
            with self.assertRaises(AssertionError):make(self.source,self.out,153,'1.0.129',1,self.key)
    def test_reject_executable_and_invalid_image(self):
        (self.source/'code.dex').write_bytes(b'dex')
        with self.assertRaises(AssertionError):make(self.source,self.out,150,'1.0.125',1,self.key)
        (self.source/'code.dex').unlink()
        p=self.source/'arena_v120'/'phone.webp'
        p.parent.mkdir()
        p.write_bytes(b'not a webp file')
        with self.assertRaises(AssertionError):make(self.source,self.out,150,'1.0.125',1,self.key)
if __name__=='__main__':unittest.main()
