# -*- coding: utf-8 -*-

from __future__ import with_statement
import unittest
import contextlib
import itertools
try:
    import json
except ImportError:
    import simplejson as json
from mock import Mock, patch

from txclib.project import Project
from txclib.config import Flipdict


class TestProject(unittest.TestCase):

    def test_extract_fields(self):
        """Test the functions that extract a field from a stats object."""
        stats = {
            'completed': '80%',
            'last_update': '00:00',
            'foo': 'bar',
        }
        self.assertEqual(
            stats['completed'], '%s%%' % Project._extract_completed(stats)
        )
        self.assertEqual(stats['last_update'], Project._extract_updated(stats))

    def test_specifying_resources(self):
        """Test the various ways to specify resources in a project."""
        p = Project(init=False)
        resources = [
            'proj1.res1',
            'proj2.res2',
            'transifex.txn',
            'transifex.txo',
        ]
        with patch.object(p, 'get_resource_list') as mock:
            mock.return_value = resources
            cmd_args = [
                'proj1.res1', '*1*', 'transifex*', '*r*',
                '*o', 'transifex.tx?', 'transifex.txn',
            ]
            results = [
                ['proj1.res1', ],
                ['proj1.res1', ],
                ['transifex.txn', 'transifex.txo', ],
                ['proj1.res1', 'proj2.res2', 'transifex.txn', 'transifex.txo', ],
                ['transifex.txo', ],
                ['transifex.txn', 'transifex.txo', ],
                ['transifex.txn', ],
                [],
            ]

            for i, arg in enumerate(cmd_args):
                resources = [arg]
                self.assertEqual(p.get_chosen_resources(resources), results[i])

            # wrong argument
            resources = ['*trasnifex*', ]
            self.assertRaises(Exception, p.get_chosen_resources, resources)


class TestProjectMinimumPercent(unittest.TestCase):
    """Test the minimum-perc option."""

    def setUp(self):
        super(TestProjectMinimumPercent, self).setUp()
        self.p = Project(init=False)
        self.p.minimum_perc = None
        self.p.resource = "resource"

    def test_cmd_option(self):
        """Test command-line option."""
        self.p.minimum_perc = 20
        results = itertools.cycle([80, 90 ])
        def side_effect(*args):
            return results.next()

        with patch.object(self.p, "get_resource_option") as mock:
            mock.side_effect = side_effect
            self.assertFalse(self.p._satisfies_min_translated({'completed': '12%'}))
            self.assertTrue(self.p._satisfies_min_translated({'completed': '20%'}))
            self.assertTrue(self.p._satisfies_min_translated({'completed': '30%'}))

    def test_global_only(self):
        """Test only global option."""
        results = itertools.cycle([80, None ])
        def side_effect(*args):
            return results.next()

        with patch.object(self.p, "get_resource_option") as mock:
            mock.side_effect = side_effect
            self.assertFalse(self.p._satisfies_min_translated({'completed': '70%'}))
            self.assertTrue(self.p._satisfies_min_translated({'completed': '80%'}))
            self.assertTrue(self.p._satisfies_min_translated({'completed': '90%'}))

    def test_local_lower_than_global(self):
        """Test the case where the local option is lower than the global."""
        results = itertools.cycle([80, 70 ])
        def side_effect(*args):
            return results.next()

        with patch.object(self.p, "get_resource_option") as mock:
            mock.side_effect = side_effect
            self.assertFalse(self.p._satisfies_min_translated({'completed': '60%'}))
            self.assertTrue(self.p._satisfies_min_translated({'completed': '70%'}))
            self.assertTrue(self.p._satisfies_min_translated({'completed': '80%'}))
            self.assertTrue(self.p._satisfies_min_translated({'completed': '90%'}))

    def test_local_higher_than_global(self):
        """Test the case where the local option is lower than the global."""
        results = itertools.cycle([60, 70 ])
        def side_effect(*args):
            return results.next()

        with patch.object(self.p, "get_resource_option") as mock:
            mock.side_effect = side_effect
            self.assertFalse(self.p._satisfies_min_translated({'completed': '60%'}))
            self.assertTrue(self.p._satisfies_min_translated({'completed': '70%'}))
            self.assertTrue(self.p._satisfies_min_translated({'completed': '80%'}))
            self.assertTrue(self.p._satisfies_min_translated({'completed': '90%'}))

    def test_local_only(self):
        """Test the case where the local option is lower than the global."""
        results = itertools.cycle([None, 70 ])
        def side_effect(*args):
            return results.next()

        with patch.object(self.p, "get_resource_option") as mock:
            mock.side_effect = side_effect
            self.assertFalse(self.p._satisfies_min_translated({'completed': '60%'}))
            self.assertTrue(self.p._satisfies_min_translated({'completed': '70%'}))
            self.assertTrue(self.p._satisfies_min_translated({'completed': '80%'}))
            self.assertTrue(self.p._satisfies_min_translated({'completed': '90%'}))

    def test_no_option(self):
        """"Test the case there is nothing defined."""
        results = itertools.cycle([None, None ])
        def side_effect(*args):
            return results.next()

        with patch.object(self.p, "get_resource_option") as mock:
            mock.side_effect = side_effect
            self.assertTrue(self.p._satisfies_min_translated({'completed': '0%'}))
            self.assertTrue(self.p._satisfies_min_translated({'completed': '10%'}))
            self.assertTrue(self.p._satisfies_min_translated({'completed': '90%'}))


class TestProjectFilters(unittest.TestCase):
    """Test filters used to decide whether to push/pull a translation or not."""

    def setUp(self):
        super(TestProjectFilters, self).setUp()
        self.p = Project(init=False)
        self.p.minimum_perc = None
        self.p.resource = "resource"
        self.stats = {
            'en': {
                'completed': '100%', 'last_update': '2011-11-01 15:00:00',
            }, 'el': {
                'completed': '60%', 'last_update': '2011-11-01 15:00:00',
            }, 'pt': {
                'completed': '70%', 'last_update': '2011-11-01 15:00:00',
            },
        }
        self.langs = self.stats.keys()

    def test_add_translation(self):
        """Test filters for adding translations.

        We do not test here for minimum percentages.
        """
        with patch.object(self.p, "get_resource_option") as mock:
            mock.return_value = None
            should_add = self.p._should_add_translation
            for force in [True, False]:
                for lang in self.langs:
                    self.assertTrue(should_add(lang, self.stats, force))

            # unknown language
            self.assertFalse(should_add('es', self.stats))

    def test_update_translation(self):
        """Test filters for updating a translation.

        We do not test here for minimum percentages.
        """
        with patch.object(self.p, "get_resource_option") as mock:
            mock.return_value = None

            should_update = self.p._should_update_translation
            force = True
            for lang in self.langs:
                self.assertTrue(should_update(lang, self.stats, 'foo', force))

            force = False       # reminder
            local_file = 'foo'

            # unknown language
            self.assertFalse(should_update('es', self.stats, local_file))

            # no local file
            with patch.object(self.p, "_get_time_of_local_file") as time_mock:
                time_mock.return_value = None
                with patch.object(self.p, "get_full_path") as path_mock:
                    path_mock.return_value = "foo"
                    for lang in self.langs:
                        self.assertTrue(
                            should_update(lang, self.stats, local_file)
                        )

            # older local files
            local_times = [self.p._generate_timestamp('2011-11-01 14:00:59')]
            results = itertools.cycle(local_times)
            def side_effect(*args):
                return results.next()

            with patch.object(self.p, "_get_time_of_local_file") as time_mock:
                time_mock.side_effect = side_effect
                with patch.object(self.p, "get_full_path") as path_mock:
                    path_mock.return_value = "foo"
                    for lang in self.langs:
                        self.assertTrue(
                            should_update(lang, self.stats, local_file)
                        )

            # newer local files
            local_times = [self.p._generate_timestamp('2011-11-01 15:01:59')]
            results = itertools.cycle(local_times)
            def side_effect(*args):
                return results.next()

            with patch.object(self.p, "_get_time_of_local_file") as time_mock:
                time_mock.side_effect = side_effect
                with patch.object(self.p, "get_full_path") as path_mock:
                    path_mock.return_value = "foo"
                    for lang in self.langs:
                        self.assertFalse(
                            should_update(lang, self.stats, local_file)
                        )

    def test_push_translation(self):
        """Test filters for pushing a translation file."""
        with patch.object(self.p, "get_resource_option") as mock:
            mock.return_value = None

            local_file = 'foo'
            should_push = self.p._should_push_translation
            force = True
            for lang in self.langs:
                self.assertTrue(should_push(lang, self.stats, local_file, force))

            force = False       # reminder

            # unknown language
            self.assertTrue(should_push('es', self.stats, local_file))

            # older local files
            local_times = [self.p._generate_timestamp('2011-11-01 14:00:59')]
            results = itertools.cycle(local_times)
            def side_effect(*args):
                return results.next()

            with patch.object(self.p, "_get_time_of_local_file") as time_mock:
                time_mock.side_effect = side_effect
                with patch.object(self.p, "get_full_path") as path_mock:
                    path_mock.return_value = "foo"
                    for lang in self.langs:
                        self.assertFalse(
                            should_push(lang, self.stats, local_file)
                        )

            # newer local files
            local_times = [self.p._generate_timestamp('2011-11-01 15:01:59')]
            results = itertools.cycle(local_times)
            def side_effect(*args):
                return results.next()

            with patch.object(self.p, "_get_time_of_local_file") as time_mock:
                time_mock.side_effect = side_effect
                with patch.object(self.p, "get_full_path") as path_mock:
                    path_mock.return_value = "foo"
                    for lang in self.langs:
                        self.assertTrue(
                            should_push(lang, self.stats, local_file)
                        )


class TestProjectPull(unittest.TestCase):
    """Test bits & pieces of the pull method."""

    def setUp(self):
        super(TestProjectPull, self).setUp()
        self.p = Project(init=False)
        self.p.minimum_perc = None
        self.p.resource = "resource"
        self.p.host = 'foo'
        self.p.project_slug = 'foo'
        self.p.resource_slug = 'foo'
        self.stats = {
            'en': {
                'completed': '100%', 'last_update': '2011-11-01 15:00:00',
            }, 'el': {
                'completed': '60%', 'last_update': '2011-11-01 15:00:00',
            }, 'pt': {
                'completed': '70%', 'last_update': '2011-11-01 15:00:00',
            },
        }
        self.langs = self.stats.keys()
        self.files = dict(zip(self.langs, itertools.repeat(None)))
        self.details = {'available_languages': []}
        for lang in self.langs:
            self.details['available_languages'].append({'code': lang})
        self.slang = 'en'
        self.lang_map = Flipdict()

    def test_new_translations(self):
        """Test finding new transaltions to add."""
        with patch.object(self.p, 'do_url_request') as resource_mock:
            resource_mock.return_value = json.dumps(self.details)
            files_keys = self.langs
            new_trans = self.p._new_translations_to_add
            for force in [True, False]:
                res = new_trans(
                    self.files, self.slang, self.lang_map, self.stats, force
                )
                self.assertEquals(res, set([]))

            with patch.object(self.p, '_should_add_translation') as filter_mock:
                filter_mock.return_value = True
                for force in [True, False]:
                    res = new_trans(
                        {'el': None}, self.slang, self.lang_map, self.stats, force
                    )
                    self.assertEquals(res, set(['pt']))
                for force in [True, False]:
                    res = new_trans(
                        {}, self.slang, self.lang_map, self.stats, force
                    )
                    self.assertEquals(res, set(['el', 'pt']))

                files = {}
                files['pt_PT'] = None
                lang_map = {'pt': 'pt_PT'}
                for force in [True, False]:
                    res = new_trans(
                        files, self.slang, lang_map, self.stats, force
                    )
                    self.assertEquals(res, set(['el']))

    def test_languages_to_pull_empty_initial_list(self):
        """Test determining the languages to pull, when the initial
        list is empty.
        """
        languages = []
        force = False

        res = self.p._languages_to_pull(
            languages, self.files, self.lang_map, self.stats, force
        )
        existing = res[0]
        new = res[1]
        self.assertEquals(existing, set(['el', 'en', 'pt']))
        self.assertFalse(new)

        del self.files['el']
        self.files['el-gr'] = None
        self.lang_map['el'] = 'el-gr'
        res = self.p._languages_to_pull(
            languages, self.files, self.lang_map, self.stats, force
        )
        existing = res[0]
        new = res[1]
        self.assertEquals(existing, set(['el', 'en', 'pt']))
        self.assertFalse(new)

    def test_languages_to_pull_with_initial_list(self):
        """Test determining the languages to pull, then there is a
        language selection from the user.
        """
        languages = ['el', 'en']
        self.lang_map['el'] = 'el-gr'
        del self.files['el']
        self.files['el-gr'] = None
        force = False

        with patch.object(self.p, '_should_add_translation') as mock:
            mock.return_value = True
            res = self.p._languages_to_pull(
                languages, self.files, self.lang_map, self.stats, force
            )
            existing = res[0]
            new = res[1]
            self.assertEquals(existing, set(['en', 'el-gr', ]))
            self.assertFalse(new)

            mock.return_value = False
            res = self.p._languages_to_pull(
                languages, self.files, self.lang_map, self.stats, force
            )
            existing = res[0]
            new = res[1]
            self.assertEquals(existing, set(['en', 'el-gr', ]))
            self.assertFalse(new)

            del self.files['el-gr']
            mock.return_value = True
            res = self.p._languages_to_pull(
                languages, self.files, self.lang_map, self.stats, force
            )
            existing = res[0]
            new = res[1]
            self.assertEquals(existing, set(['en', ]))
            self.assertEquals(new, set(['el', ]))

            mock.return_value = False
            res = self.p._languages_to_pull(
                languages, self.files, self.lang_map, self.stats, force
            )
            existing = res[0]
            new = res[1]
            self.assertEquals(existing, set(['en', ]))
            self.assertEquals(new, set([]))

    def test_in_combination_with_force_option(self):
        """Test the minumum-perc option along with -f."""
        with patch.object(self.p, 'get_resource_option') as mock:
            mock.return_value = 70

            res = self.p._should_download('de', self.stats, None, False)
            self.assertEquals(res, False)
            res = self.p._should_download('el', self.stats, None, False)
            self.assertEquals(res, False)
            res = self.p._should_download('el', self.stats, None, True)
            self.assertEquals(res, False)
            res = self.p._should_download('en', self.stats, None, False)
            self.assertEquals(res, True)
            res = self.p._should_download('en', self.stats, None, True)
            self.assertEquals(res, True)

            with patch.object(self.p, '_remote_is_newer') as local_file_mock:
                local_file_mock = False
                res = self.p._should_download('pt', self.stats, None, False)
                self.assertEquals(res, True)
                res = self.p._should_download('pt', self.stats, None, True)
                self.assertEquals(res, True)


class TestFormats(unittest.TestCase):
    """Tests for the supported formats."""

    def setUp(self):
        self.p = Project(init=False)

    def test_extensions(self):
        """Test returning the correct extension for a format."""
        sample_formats = {
            'PO': {'file-extensions': '.po, .pot'},
            'QT': {'file-extensions': '.ts'},
        }
        extensions = ['.po', '.ts', '', ]
        with patch.object(self.p, "do_url_request") as mock:
            mock.return_value = json.dumps(sample_formats)
            for (type_, ext) in zip(['PO', 'QT', 'NONE', ], extensions):
                extension = self.p._extension_for(type_)
                self.assertEquals(extension, ext)


class TestOptions(unittest.TestCase):
    """Test the methods related to parsing the configuration file."""

    def setUp(self):
        self.p = Project(init=False)

    def test_get_option(self):
        """Test _get_option method."""
        with contextlib.nested(
            patch.object(self.p, 'get_resource_option'),
            patch.object(self.p, 'config', create=True)
        ) as (rmock, cmock):
            rmock.return_value = 'resource'
            cmock.has_option.return_value = 'main'
            cmock.get.return_value = 'main'
            self.assertEqual(self.p._get_option(None, None), 'resource')
            rmock.return_value = None
            cmock.has_option.return_value = 'main'
            cmock.get.return_value = 'main'
            self.assertEqual(self.p._get_option(None, None), 'main')
            cmock.has_option.return_value = None
            self.assertIs(self.p._get_option(None, None), None)


class TestConfigurationOptions(unittest.TestCase):
    """Test the various configuration options."""

    def test_i18n_type(self):
        p = Project(init=False)
        type_string = 'type'
        i18n_type = 'PO'
        with patch.object(p, 'config', create=True) as config_mock:
            p.set_i18n_type([], i18n_type)
            calls = config_mock.method_calls
            self.assertEquals('set', calls[0][0])
            self.assertEquals('main', calls[0][1][0])
            p.set_i18n_type(['transifex.txo'], 'PO')
            calls = config_mock.method_calls
            self.assertEquals('set', calls[0][0])
            p.set_i18n_type(['transifex.txo', 'transifex.txn'], 'PO')
            calls = config_mock.method_calls
            self.assertEquals('set', calls[0][0])
            self.assertEquals('set', calls[1][0])


class TestStats(unittest.TestCase):
    """Test the access to the stats objects."""

    def setUp(self):
        self.stats = Mock()
        self.stats.__getitem__ = Mock()
        self.stats.__getitem__.return_value = '12%'

    def test_field_used_per_mode(self):
        """Test the fields used for each mode."""
        Project._extract_completed(self.stats, 'translate')
        self.stats.__getitem__.assert_called_with('completed')
        Project._extract_completed(self.stats, 'reviewed')
        self.stats.__getitem__.assert_called_with('reviewed_percentage')

