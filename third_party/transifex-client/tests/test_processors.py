# -*- coding: utf-8 -*-

"""
Unit tests for processor functions.
"""

import unittest
from urlparse import urlparse
from txclib.processors import hostname_tld_migration, hostname_ssl_migration


class TestHostname(unittest.TestCase):
    """Test for hostname processors."""

    def test_tld_migration_needed(self):
        """
        Test the tld migration of Transifex, when needed.
        """
        hostnames = [
            'http://transifex.net', 'http://www.transifex.net',
            'https://fedora.transifex.net',
        ]
        for h in hostnames:
            hostname = hostname_tld_migration(h)
            self.assertTrue(hostname.endswith('com'))
        orig_hostname = 'http://www.transifex.net/path/'
        hostname = hostname_tld_migration(orig_hostname)
        self.assertEqual(hostname, orig_hostname.replace('net', 'com', 1))

    def test_tld_migration_needed(self):
        """
        Test that unneeded tld migrations are detected correctly.
        """
        hostnames = [
            'https://www.transifex.com', 'http://fedora.transifex.com',
            'http://www.example.net/path/'
        ]
        for h in hostnames:
            hostname = hostname_tld_migration(h)
            self.assertEqual(hostname, h)

    def test_no_scheme_specified(self):
        """
        Test that, if no scheme has been specified, the https one will be used.
        """
        hostname = '//transifex.net'
        hostname = hostname_ssl_migration(hostname)
        self.assertTrue(hostname.startswith('https://'))

    def test_http_replacement(self):
        """Test the replacement of http with https."""
        hostnames = [
            'http://transifex.com', 'http://transifex.net/http/',
            'http://www.transifex.com/path/'
        ]
        for h in hostnames:
            hostname = hostname_ssl_migration(h)
            self.assertEqual(hostname[:8], 'https://')
            self.assertEqual(hostname[7:], h[6:])

    def test_no_http_replacement_needed(self):
        """Test that http will not be replaces with https, when not needed."""
        for h in ['http://example.com', 'http://example.com/http/']:
            hostname = hostname_ssl_migration(h)
            self.assertEqual(hostname, hostname)
