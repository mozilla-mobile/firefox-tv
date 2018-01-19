#!/usr/bin/env python3
"""
Intended to help convert machine translations, created by a YAML format, into
strings.xml files in a robust, error-free way.

To run:
    # You must be in the right working directory!
    pushd tools/l10n/robotranslations/

    # Create a virtualenv
    virtualenv --python=python3 venv
    source venv/bin/activate

    # Install dependencies
    pip install -r requirements.txt

    # Run!
    ./save_robotranslations.py  # reads ./locales.yaml

    # Cleanup
    deactivate
    popd

The script takes a YAML file of the format:

```yaml
titles:
    - string_title1
    - string_title2
    ...

locale_name:
    - string_translation1
    - string_translation2
    ...
```

And puts it into the appropriate strings.xml files in an android project, e.g.
Korean strings would go to res/values-ko/strings.xml:
```xml
    <!-- Appended by save_robotranslations.py -->
    <string name="string_title1">string_translation1</string>
    <string name="string_title2">string_translation2</string>
</resources>
```
"""

import os.path
import sys
import yaml
from pprint import pprint as pp

_INPUT_FILE = 'locales.yaml'
_RES_DIR = '../../../app/src/main/res/'

# like commit 36673ae6196b4f17750e2828a20923cdc0cb600f
_TITLE_LOCALE = 'titles'
KNOWN_LOCALES = {
        'en-US',
        'de',
        'zh-rCN',
        'fr',
        'hi-rIN',
        'ja',
        'ko',
        'ru',
        'es-rES',
        'it',  # Not in original commit.
        # Missing ar
}

_NAME = os.path.basename(__file__)

def get_translations_from_yaml(path):
    with open(path) as f:
        data = yaml.load(f)

    string_names = data[_TITLE_LOCALE]
    locales = set(data.keys()) - set([_TITLE_LOCALE])

    # Assert locales are plausible.
    unknown_locales = locales - KNOWN_LOCALES
    if len(unknown_locales) > 0:
        pp('locales yaml contains unknown locale(s):', stream=sys.stderr)
        pp(unknown_locales, stream=sys.stderr)
        sys.exit(1)

    # Assert each locale has correct number of translations.
    expected_translation_count = len(string_names)
    for locale in locales:
        if len(data[locale]) != expected_translation_count:
            print('locale contains more/less transalations than title key', file=sys.stderr)
            sys.exit(1)

    # Output: {'locale': [(name, text), ...], ...}
    for locale in locales:
        translations = data[locale]
        data[locale] = list(zip(string_names, translations))

    data.pop(_TITLE_LOCALE, None)  # Rm special locale.
    return data


# A more robust implementation would use an XML parser but unfortunately,
# Python's default parser removes comments and workarounds require there to
# be only 1 root XML node and we have two: the content and a comment.
# Instead, we just append plaintext.
def append_translations(path, append_list):
    format_str = '    <string name="{}">{}</string>\n'
    translated_str = [format_str.format(title, text) for [title, text] in append_list]

    updated_lines = []
    with open(path) as f:
        for line in f:
            if line.strip() == '</resources>':  # At last line.
                updated_lines.append('\n    <!-- Appended by ' + _NAME + ' -->\n')
                updated_lines.extend(translated_str)

            updated_lines.append(line)  # Append existing line.

    with open(path, 'w') as f:
        f.write(''.join(updated_lines))


def main():
    locale_to_translation = get_translations_from_yaml(_INPUT_FILE)
    #import pdb; pdb.set_trace()
    for (locale, translations) in locale_to_translation.items():
        dir_str = 'values' + ('-' + locale if locale != 'en-US' else '')
        path = _RES_DIR + dir_str + '/strings.xml'
        append_translations(path, translations)


if __name__ == '__main__':
    main()
