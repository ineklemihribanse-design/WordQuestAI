#!/usr/bin/env python3
"""Activate the *actual user-supplied* 1.0.75 ZIP during existing CI assembly.

Absent ZIP is a clean no-op. This intentionally does not fabricate replacements,
install cropped fallback artwork, change navigation, or touch signing/versioning.
"""
from pathlib import Path, PurePosixPath
import sys
import zipfile

ROOT = Path(__file__).resolve().parents[2]
ARCHIVE = ROOT / 'tools/v175/WordQuestAI_175_Gorsel_Entegrasyon_Hazirlik.zip'
SRC = ROOT / 'source/app/src/main'
JAVA = SRC / 'java/com/kelimebulmacasi/a1'
ALLOWED = {'icons', 'backgrounds', 'banners', 'cards', 'buttons', 'scenes'}
PREFIX = 'assets/wq175_user_art/'


def replace_once(content: str, old: str, new: str, label: str) -> str:
    if content.count(old) != 1:
        raise AssertionError(f'{label}: expected one source signature, found {content.count(old)}')
    return content.replace(old, new, 1)


def main() -> int:
    if not ARCHIVE.is_file():
        print('INFO: approved ZIP not committed; using existing v175 premium artwork, without synthetic replacements')
        return 0
    if not JAVA.is_dir():
        raise AssertionError('Expected assembled source/app tree before asset patch')

    with zipfile.ZipFile(ARCHIVE) as z:
        names = set(z.namelist())
        assert {'Wq175UserArt.kt', 'asset-manifest.json'}.issubset(names)
        items = []
        for name in names:
            if not name.startswith(PREFIX) or name.endswith('/'):
                continue
            relative = PurePosixPath(name[len(PREFIX):])
            if (len(relative.parts) != 2 or relative.parts[0] not in ALLOWED
                    or relative.suffix != '.webp'
                    or any(part in ('', '..') for part in relative.parts)):
                continue  # intentionally ignore low-quality 'extra' atlas crops
            data = z.read(name)
            assert len(data) > 200 and data[:4] == b'RIFF' and data[8:12] == b'WEBP', name
            items.append((relative, data))
        icons = sum(p.parts[0] == 'icons' for p, _ in items)
        assert icons >= 102, f'Expected 102 independent icons, got {icons}'

        # Validate patch signatures before writing anything.
        icons_file = JAVA / 'WordQuest175Icons.kt'
        theme_file = JAVA / 'WordQuest175ThemeArt.kt'
        original_icons = icons_file.read_text('utf-8')
        original_theme = theme_file.read_text('utf-8')
        patched_icons = replace_once(
            original_icons,
            '    if (approved != null) {\n        Image(painter = painterResource(approved)',
            '''    val userIcon = remember(context, name) {
        wq175LoadUserArt(context, "icons", Wq175UserArtMap.icon(name))
    }
    if (userIcon != null) {
        Image(bitmap = userIcon, contentDescription = null,
            modifier = modifier, contentScale = ContentScale.Fit)
    } else if (approved != null) {
        Image(painter = painterResource(approved)''',
            'imported icons override built-in approved fallback')
        old_theme = '''    Image(painter=painterResource(Wq175ThemeArt.icon(slug)),
        contentDescription=null,modifier=modifier,contentScale=ContentScale.Fit)'''
        new_theme = '''    val context = LocalContext.current
    val userIcon = remember(context, slug) { wq175LoadUserArt(context, "icons", slug) }
    if (userIcon != null) {
        Image(bitmap=userIcon,contentDescription=null,
            modifier=modifier,contentScale=ContentScale.Fit)
    } else {
        Image(painter=painterResource(Wq175ThemeArt.icon(slug)),
            contentDescription=null,modifier=modifier,contentScale=ContentScale.Fit)
    }'''
        patched_theme = replace_once(original_theme, old_theme, new_theme,
                                     'premium room icon fallback')
        patched_theme = replace_once(patched_theme,
            'import androidx.compose.runtime.Composable',
            'import androidx.compose.runtime.Composable\nimport androidx.compose.runtime.remember\nimport androidx.compose.ui.platform.LocalContext',
            'import composable loader dependencies')
        art_source = z.read('Wq175UserArt.kt').decode('utf-8')
        assert 'internal fun wq175LoadUserArt(' in art_source

        destination = SRC / 'assets/wq175_user_art'
        for path, data in items:
            out = destination.joinpath(*path.parts)
            out.parent.mkdir(parents=True, exist_ok=True)
            out.write_bytes(data)
        icons_file.write_text(patched_icons, 'utf-8')
        theme_file.write_text(patched_theme, 'utf-8')
        (JAVA / 'Wq175UserArt.kt').write_text(art_source, 'utf-8')
        print(f'PASS: installed {len(items)} user-approved WebP files, including {icons} independent icons')
        print('PASS: cropped fallback atlas excluded; navigation/Memory/Lena/versions/signing unchanged')
    return 0


if __name__ == '__main__':
    sys.exit(main())
