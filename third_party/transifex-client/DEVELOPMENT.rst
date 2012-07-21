Releasing
=========

To create a new release:

1. Update local rep and update the version in ``setup.py``::

    $ hg pull -u
    $ vim setup.py

2. Test::

    $ python setup.py clean sdist
    $ cd dist
    $ tar zxf ...
    $ cd transifex-client
    ...test

3. Package and upload on PyPI::

    $ python setup.py clean sdist bdist_egg upload
