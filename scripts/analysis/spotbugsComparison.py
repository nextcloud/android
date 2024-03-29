#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2017-2024 Nextcloud GmbH and Nextcloud contributors
# SPDX-FileCopyrightText: 2017 Tobias Kaminsky <tobias@kaminsky.me>
# SPDX-License-Identifier: GPL-3.0-or-later
import argparse
import defusedxml.ElementTree as ET

import spotbugsSummary


def print_comparison(old: dict, new: dict, link_base: str, link_new: str):
    all_keys = sorted(set(list(old.keys()) + list(new.keys())))

    output = "<table><tr><th>Category</th>"
    old_header = f"<a href='{link_base}'>Base</a>" if link_base is not None else "Base"
    output += f"<th>{old_header}</th>"
    new_header = f"<a href='{link_new}'>New</a>" if link_new is not None else "New"
    output += f"<th>{new_header}</th>"
    output += "</tr>"

    for category in all_keys:
        category_count_old = old[category] if category in old else 0
        category_count_new = new[category] if category in new else 0
        new_str = f"<b>{category_count_new}</b>" if category_count_new != category_count_old else str(category_count_new)
        output += "<tr>"
        output += f"<td>{category}</td>"
        output += f"<td>{category_count_old}</td>"
        output += f"<td>{new_str}</td>"
        output += "</tr>"

    output += "<tr>"
    output += "<td><b>Total</b></td>"
    output += f"<td><b>{sum(old.values())}</b></td>"
    output += f"<td><b>{sum(new.values())}</b></td>"
    output += "</tr>"

    output += "</table>"

    print(output)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("base_file", help="base file for comparison")
    parser.add_argument("new_file", help="new file for comparison")
    parser.add_argument("--link-base", help="http link to base html report")
    parser.add_argument("--link-new", help="http link to new html report")
    args = parser.parse_args()

    base_tree = ET.parse(args.base_file)
    base_summary = spotbugsSummary.get_counts(base_tree)

    new_tree = ET.parse(args.new_file)
    new_summary = spotbugsSummary.get_counts(new_tree)

    print_comparison(base_summary, new_summary, args.link_base, args.link_new)
