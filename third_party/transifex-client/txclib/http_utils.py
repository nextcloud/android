# -*- coding: utf-8 -*-

"""
HTTP-related utility functions.
"""

from __future__ import with_statement
import gzip
try:
    import cStringIO as StringIO
except ImportError:
    import StringIO


def _gzip_decode(gzip_data):
    """
    Unzip gzipped data and return them.

    :param gzip_data: Gzipped data.
    :returns: The actual data.
    """
    try:
        gzip_data = StringIO.StringIO(gzip_data)
        gzip_file = gzip.GzipFile(fileobj=gzip_data)
        data = gzip_file.read()
        return data
    finally:
        gzip_data.close()


def http_response(response):
    """
    Return the response of a HTTP request.

    If the response has been gzipped, gunzip it first.

    :param response: The raw response of a HTTP request.
    :returns: A response suitable to be used by clients.
    """
    metadata = response.info()
    data = response.read()
    response.close()
    if metadata.get('content-encoding') == 'gzip':
        return _gzip_decode(data)
    else:
        return data
