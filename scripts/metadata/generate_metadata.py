#!/usr/bin/env python3

# Author: Torsten Grote
# License: GPLv3 or later
# copied on 2017/11/06 from https://github.com/grote/Transportr/blob/master/fastlane/generate_metadata.py
# adapted by Tobias Kaminsky

import codecs
import os
import shutil
from xml.etree import ElementTree

XML_PATH = '../../src/main/res'
METADATA_PATH = '../../src/generic/fastlane/metadata/android/'
METADATA_DEV_PATH = '../../src/versionDev/fastlane/metadata/android/'
DEFAULT_LANG = 'en-US'
LANG_MAP = {
    'values': 'en-US',
    'values-en-rGB': 'en-GB',
    'values-ca': 'ca',
    'values-cs': 'cs-CZ',
    'values-de': 'de-DE',
    'values-es': 'es-ES',
    'values-fr': 'fr-FR',
    'values-hu': 'hu-HU',
    'values-it': 'it-IT',
    'values-pt-rBR': 'pt-BR',
    'values-pt-rPT': 'pt-PT',
    'values-ta': 'ta-IN',
    'values-sv': 'sv-SE',
    'values-sq-rAL': 'sq-AL',
    'values-sq-rMK': 'sq-MK',
    'values-iw-rIL': 'iw-IL',
    'values-ar': 'ar-AR',
    'values-bg-rBG': 'bg-BG',
    'values-da': 'da-DK',
    'values-fi-rFI': 'fi-FI',
    'values-gl-rES': 'gl-ES',
    'values-tr': 'tr-TR',
    'values-uk': 'uk-UK',
    'values-vi': 'vi-VI',
    'values-ro': 'ro-RO',
    'values-ou': 'ru-RU',
    'values-sr': 'sr-SR',
    'values-pl': 'pl-PL',
    'values-el': 'el-GR',
    'values-ko': 'ko-KR',
    'values-nl': 'nl-NL',
    'values-ja': 'ja-JP',
    'values-no-rNO': 'no-NO',
    'values-eu': 'eu-ES',
    'values-lt-rLT': 'lt-LT',
    'values-zh-rKN': 'zh-HK',
    'values-zk': 'zk-CN',
    'values-is': 'is-IS',
    'values-id': 'id-ID',
    'values-cs-rCZ': 'cs-CZ',
    'values-sl': 'sl-SL',
    'values-fa': 'fa-FA'
}

PATH = os.path.dirname(os.path.realpath(__file__))


def main():
    path = os.path.join(PATH, XML_PATH)
    for entry in os.listdir(path):
        directory = os.path.join(path, entry)
        if not os.path.isdir(directory) or entry not in LANG_MAP.keys():
            continue
        strings_file = os.path.join(directory, 'strings.xml')
        if not os.path.isfile(strings_file):
            print("Error: %s does not exist" % strings_file)
            continue

        print()
        print(LANG_MAP[entry])
        print("Parsing %s..." % strings_file)
        e = ElementTree.parse(strings_file).getroot()
        short_desc = e.find('.//string[@name="store_short_desc"]')
        full_desc = e.find('.//string[@name="store_full_desc"]')
        short_dev_desc = e.find('.//string[@name="store_short_dev_desc"]')
        full_dev_desc = e.find('.//string[@name="store_full_dev_desc"]')
        if short_desc is not None:
            save_file(short_desc.text, LANG_MAP[entry], 'short_description.txt', False)
        if short_dev_desc is not None:
            save_file(short_dev_desc.text, LANG_MAP[entry], 'short_description.txt', True)
        if full_desc is not None:
            save_file(full_desc.text, LANG_MAP[entry], 'full_description.txt', False)
        if full_dev_desc is not None:
            save_file(full_dev_desc.text, LANG_MAP[entry], 'full_description.txt', True)


def save_file(text, directory, filename, dev):
    if dev:
        directory_path = os.path.join(PATH, METADATA_DEV_PATH, directory)
    else:
        directory_path = os.path.join(PATH, METADATA_PATH, directory)

    if not os.path.exists(directory_path):
        os.makedirs(directory_path)
    if filename == 'short_description.txt':
        limit = 80
    else:
        limit = 0
    text = clean_text(text, limit)
    check_title(directory_path)
    file_path = os.path.join(directory_path, filename)
    print("Writing %s..." % file_path)
    with codecs.open(file_path, 'w', 'utf-8') as f:
        f.write(text)


def clean_text(text, limit=0):
    text = text.replace('\\\'', '\'').replace('\\n', '\n')
    if limit != 0 and len(text) > limit:
        print("Warning: Short Description longer than 80 characters, truncating...")
        text = text[:limit]
    return text


def check_title(directory):
    title_path = os.path.join(directory, 'title.txt')
    if os.path.exists(title_path):
        return
    default_title_path = os.path.join(directory, '..', DEFAULT_LANG, 'title.txt')
    shutil.copy(default_title_path, title_path)


if __name__ == "__main__":
    main()
