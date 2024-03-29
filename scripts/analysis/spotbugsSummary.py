#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2017-2024 Nextcloud GmbH and Nextcloud contributors
# SPDX-FileCopyrightText: 2017 Tobias Kaminsky <tobias@kaminsky.me>
# SPDX-License-Identifier: GPL-3.0-or-later
import argparse
import defusedxml.ElementTree as ET


def get_counts(tree):
    category_counts = {}
    category_names = {}
    for child in tree.getroot():
        if child.tag == "BugInstance":
            category = child.attrib['category']
            if category in category_counts:
                category_counts[category] = category_counts[category] + 1
            else:
                category_counts[category] = 1
        elif child.tag == "BugCategory":
            category = child.attrib['category']
            category_names[category] = child[0].text

    summary = {}
    for category in category_counts.keys():
        summary[category_names[category]] = category_counts[category]
    return summary


def print_html(summary):
    output = "<table><tr><th>Category</th><th>Count</th></tr>"

    categories = sorted(summary.keys())
    for category in categories:
        output += "<tr>"
        output += f"<td>{category}</td>"
        output += f"<td>{summary[category]}</td>"
        output += "</tr>"

    output += "<tr>"
    output += "<td><b>Total</b></td>"
    output += f"<td><b>{sum(summary.values())}</b></td>"
    output += "</tr>"

    output += "</table>"

    print(output)


def print_total(summary):
    print(sum(summary.values()))


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--total", help="print total count instead of summary HTML",
                        action="store_true")
    parser.add_argument("--file", help="file to parse", default="app/build/reports/spotbugs/gplayDebug.xml")
    args = parser.parse_args()
    tree = ET.parse(args.file)
    summary = get_counts(tree)
    if args.total:
        print_total(summary)
    else:
        print_html(summary)
