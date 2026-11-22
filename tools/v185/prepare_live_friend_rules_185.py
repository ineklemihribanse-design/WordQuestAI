#!/usr/bin/env python3
"""Generate minimal and auditable Firebase v1.0.85 friend-preview rules.

Offline only: never connects to Firebase, never alters original export.
"""
import argparse
import copy
import hashlib
import json
from pathlib import Path

FRIEND_READ = (
    "auth != null && (auth.uid === $uid || "
    "(root.child('friends').child(auth.uid).child($uid).val() === true && "
    "root.child('friends').child($uid).child(auth.uid).val() === true))"
)
PREVIEW_RULE = {
    ".validate": (
        "newData.isString() && newData.val().length >= 140 && "
        "newData.val().length <= 18050 && "
        r"newData.val().matches(/^custom_[a-f0-9]{32}:[A-Za-z0-9+\\/=]+$/)"
    )
}

def digest(data):
    encoded = json.dumps(data, sort_keys=True, separators=(',', ':')).encode()
    return hashlib.sha256(encoded).hexdigest()

def merged_live_rules(live):
    if not isinstance(live, dict) or not isinstance(live.get('rules'), dict):
        raise ValueError('Geçerli bir Firebase Realtime Database rules JSON dosyası değil.')
    node = live['rules'].get('friendProfiles', {}).get('$uid')
    if not isinstance(node, dict):
        raise ValueError('friendProfiles/$uid bulunamadı. Kuralları değiştirmeyin.')
    if node.get('.read') != FRIEND_READ:
        raise ValueError('Karşılıklı-arkadaş okuma kuralı farklı; otomatik değişiklik yok.')
    if node.get('.write') != 'auth != null && auth.uid === $uid':
        raise ValueError('Sahip-yazma kuralı farklı; otomatik değişiklik yok.')
    if node.get('$other') != {'.validate': False}:
        raise ValueError('Bilinmeyen alanları reddeden kural farklı; işlem yok.')
    if 'avatarPreview' in node and node['avatarPreview'] != PREVIEW_RULE:
        raise ValueError('avatarPreview kuralı beklenenden farklı; işlem yok.')
    merged = copy.deepcopy(live)
    merged['rules']['friendProfiles']['$uid']['avatarPreview'] = PREVIEW_RULE
    before = copy.deepcopy(live)
    before['rules']['friendProfiles']['$uid'].pop('avatarPreview', None)
    check = copy.deepcopy(merged)
    check['rules']['friendProfiles']['$uid'].pop('avatarPreview')
    if check != before:
        raise RuntimeError('Başka bir Firebase kuralı değişti; işlem iptal edildi.')
    return merged

def main():
    p = argparse.ArgumentParser(description='Prepare safe, friend-only, optional avatarPreview rule')
    p.add_argument('--live', required=True, type=Path, help='Current rules exported from Firebase Console')
    p.add_argument('--output', required=True, type=Path, help='New output path that does not yet exist')
    args = p.parse_args()
    if args.live.resolve() == args.output.resolve() or args.output.exists():
        p.error('Orijinal kuralları koruyun: çıktı mevcut dosyanın üzerine yazılamaz.')
    live = json.loads(args.live.read_text('utf-8'))
    merged = merged_live_rules(live)
    print('Current SHA-256:', digest(live))
    print('Merged  SHA-256:', digest(merged))
    print('No change needed.' if merged == live else 'PASS: only optional friend preview validator changed.')
    with args.output.open('x', encoding='utf-8') as f:
        json.dump(merged, f, ensure_ascii=False, indent=2)
        f.write('\n')
    print('Manual review required before publishing:', args.output)
    print('No live Firebase deployment performed.')

if __name__ == '__main__':
    main()
