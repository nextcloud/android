# -*- coding: utf-8 -*-

"""
Exception classes for the tx client.
"""


class UnInitializedError(Exception):
    """The project directory has not been initialized."""


class UnknownCommandError(Exception):
    """The provided command is not supported."""
