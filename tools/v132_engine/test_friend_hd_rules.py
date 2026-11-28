import unittest,copy
from prepare_friend_hd_rules import merge,FRIEND_READ,OWNER,NODE
class FriendHdRules(unittest.TestCase):
    def fixture(self):
        return {"rules":{".read":False,".write":False,"friendProfiles":{"$uid":{".read":FRIEND_READ,".write":OWNER,"avatarPreview":{".validate":"existing"}}},"rooms":{"$room":{"test":True}}}}
    def test_only_new_node(self):
        x=self.fixture();before=copy.deepcopy(x);out=merge(x)
        self.assertEqual(x,before);self.assertEqual(out["rules"].pop("friendMascotHd"),NODE);self.assertEqual(out,x)
    def test_refuse_root_grant(self):
        x=self.fixture();x["rules"][".read"]=True
        with self.assertRaises(ValueError):merge(x)
    def test_refuse_wrong_owner(self):
        x=self.fixture();x["rules"]["friendProfiles"]["$uid"][".write"]=True
        with self.assertRaises(ValueError):merge(x)
    def test_revocation_binding_and_limits(self):
        n=NODE["$uid"];self.assertIn("data.child('preview').val() === root.child('friendProfiles')",n[".read"])
        self.assertIn("160100",n["hd"][".validate"]);self.assertEqual(n["$other"][".validate"],False)
if __name__=="__main__":unittest.main()
