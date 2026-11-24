from pathlib import Path
import hashlib,json,struct,zipfile,copy,sys
import numpy as np
BASE='cb033dc3503f01e3fa64971c2bbe40b1260e7d9a96f7860796a32400328ad26f'
DONOR='ff5989c5056596fc487973e87974794603981890fec8a3dcb83561683f4f3a4d'
NAMES=['Viseme_AA','Viseme_EE','Viseme_OO','Viseme_UU','Smile','Blink']
def read(raw):
    magic,version,total,n,typ=struct.unpack_from('<4s4I',raw)
    assert magic==b'glTF' and version==2 and total==len(raw) and typ==0x4e4f534a
    d=json.loads(raw[20:20+n]);length,typ=struct.unpack_from('<II',raw,20+n)
    assert typ==0x004e4942
    return d,raw[28+n:28+n+length]
def array(d,b,i):
    a=d['accessors'][i];v=d['bufferViews'][a['bufferView']]
    count={'SCALAR':1,'VEC3':3}[a['type']];dtype=np.dtype({5126:'<f4',5123:'<u2',5125:'<u4',5121:'u1'}[a['componentType']])
    assert not a.get('sparse') and v.get('buffer',0)==0
    return np.ndarray((a['count'],count),dtype=dtype,buffer=b,offset=v.get('byteOffset',0)+a.get('byteOffset',0),strides=(v.get('byteStride',dtype.itemsize*count),dtype.itemsize))
def sha(b):return hashlib.sha256(b).hexdigest()
def build(base,donor):
    assert sha(base)==BASE and sha(donor)==DONOR
    d,bin0=read(base);v,db=read(donor);original=copy.deepcopy(d)
    assert len(d['meshes'])==1 and len(d['skins'][0]['joints'])==65 and len(d['animations'])==38 and len(d['materials'])==9
    mesh=d['meshes'][0];don=v['meshes'][0];p=mesh['primitives'];q=don['primitives'][0]
    x=array(d,bin0,p[0]['attributes']['POSITION']);y=array(v,db,q['attributes']['POSITION'])
    assert x.shape==y.shape==(31958,3)
    assert all(i['attributes']['POSITION']==p[0]['attributes']['POSITION'] for i in p)
    a=np.column_stack([y,np.ones(len(y))]);aff=np.linalg.lstsq(a,x,rcond=None)[0]
    assert np.max(np.abs(a@aff-x))<2e-6 and np.max(np.abs(aff[:3]-np.eye(3)*.9500907))<1e-5
    def triangles(a):
        a=np.sort(a.reshape(-1,3),axis=1);return a[np.lexsort(a.T)]
    assert np.array_equal(triangles(np.concatenate([array(d,bin0,i['indices']) for i in p])),triangles(array(v,db,q['indices'])))
    bymat={d['materials'][i['material']]['name']:np.unique(array(d,bin0,i['indices']).ravel()) for i in p}
    payload=bytearray(bin0);targets=[];report={}
    for name in NAMES:
        raw=array(v,db,q['targets'][don['extras']['targetNames'].index(name)]['POSITION'])
        delta=(raw*np.float32(.9500907)).astype('<f4')
        sel=np.linalg.norm(delta,axis=1)>1e-7
        allowed=np.zeros(len(x),bool);allowed[bymat['Lena_Body']]=True
        if name=='Blink':allowed[bymat['Lena_Eyelash']]=True
        assert np.all(allowed[sel]) and 200<sel.sum()<3000 and np.all(np.isfinite(delta))
        assert x[sel,1].min()>1.44 and x[sel,1].max()<1.61 and np.max(np.abs(x[sel,0]))<.075 and x[sel,2].min()>.025
        delta*=.82 if name=='Blink' else (.70 if name=='Smile' else .90)
        assert np.max(np.linalg.norm(delta,axis=1))<.016
        start=len(payload);payload.extend(delta.tobytes())
        vi=len(d['bufferViews']);d['bufferViews'].append({'buffer':0,'byteOffset':start,'byteLength':delta.nbytes,'target':34962})
        ai=len(d['accessors']);d['accessors'].append({'bufferView':vi,'componentType':5126,'count':len(x),'type':'VEC3','min':delta.min(0).tolist(),'max':delta.max(0).tolist()})
        targets.append({'POSITION':ai});report[name]={'affected':int(sel.sum()),'max_delta_m':float(np.max(np.linalg.norm(delta,axis=1)))}
    for prim in p:prim['targets']=copy.deepcopy(targets)
    mesh['weights']=[0.0]*len(NAMES);mesh['extras']={'targetNames':NAMES}
    d['buffers'][0]['byteLength']=len(payload)
    restored=copy.deepcopy(d);restored['accessors']=restored['accessors'][:len(original['accessors'])];restored['bufferViews']=restored['bufferViews'][:len(original['bufferViews'])];restored['buffers']=original['buffers'];restored['meshes']=original['meshes']
    assert restored==original and bytes(payload[:len(bin0)])==bin0
    assert all(not any(c['target']['path']=='weights' for c in a['channels']) for a in d['animations'])
    js=json.dumps(d,separators=(',',':')).encode();js+=b' '*((-len(js))%4);payload.extend(b'\0'*((-len(payload))%4))
    result=struct.pack('<4sII',b'glTF',2,12+8+len(js)+8+len(payload))+struct.pack('<I4s',len(js),b'JSON')+js+struct.pack('<I4s',len(payload),b'BIN\0')+payload
    assert sha(result)=='7cb8bd4fd09fd34b0a28a67363881c43e8af5420ce23412e139b14c957159bc5'
    print(json.dumps({'base':BASE,'result':sha(result),'morphs':report},indent=2))
    return result
if __name__=='__main__':
    a=Path(sys.argv[1]);z=Path(sys.argv[2]);out=Path(sys.argv[3])
    assert sha(z.read_bytes())=='10e758d40573bcc8d389817f44fc6b28b5024b6b7c82b3dc344b2e62599fa070'
    with zipfile.ZipFile(z) as f:don=f.read('Lena_V5_Runtime.glb')
    out.write_bytes(build(a.read_bytes(),don))
