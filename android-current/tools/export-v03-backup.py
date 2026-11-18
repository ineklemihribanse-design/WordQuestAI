import argparse
import json
import re
import subprocess
import xml.etree.ElementTree as ET
from pathlib import Path


def export_backup(profile):
    preferences = subprocess.check_output(
        ['adb', 'exec-out', 'run-as', 'com.kelimebulmacasi.a1', 'cat', f'shared_prefs/{profile}.xml']
    )
    entries = {item.attrib['name']: item for item in ET.fromstring(preferences)}
    last_game = entries.get('last_game')
    last_game_at = entries.get('last_game_at')
    return {
        'format': 'kelime-bulmacasi-progress',
        'version': 1,
        'games': [json.loads(item.text) for name, item in entries.items() if name.startswith('custom:')],
        'progress': {
            name.removeprefix('progress:'): json.loads(item.text)
            for name, item in entries.items()
            if name.startswith('progress:')
        },
        'lastGame': last_game.text if last_game is not None else None,
        'lastGameAt': int(last_game_at.attrib['value']) if last_game_at is not None else 0,
    }


def main():
    parser = argparse.ArgumentParser(description='Kelime Bulmacası 0.3 kayıtlarını JSON dosyasına alır.')
    parser.add_argument('target', nargs='?', default='Kelime_Bulmacasi_Yedek.json')
    parser.add_argument('profile', nargs='?', default='player_v2_guest')
    args = parser.parse_args()
    if not re.fullmatch(r'player_v2_(guest|account_[A-Za-z0-9_-]+)', args.profile):
        parser.error('Kayıt profili geçersiz.')
    try:
        backup = export_backup(args.profile)
        with Path(args.target).open('x', encoding='utf-8') as handle:
            json.dump(backup, handle, ensure_ascii=False)
    except (OSError, ValueError, subprocess.CalledProcessError, ET.ParseError) as error:
        parser.exit(1, f'Yedek kaydedilemedi. {error}\n')
    print(f"{args.target} kaydedildi. {len(backup['games'])} kişisel bulmaca, {len(backup['progress'])} ilerleme kaydı.")


if __name__ == '__main__':
    main()
