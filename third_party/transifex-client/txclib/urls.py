# These are the Transifex API urls

API_URLS = {
    'get_resources': '%(hostname)s/api/2/project/%(project)s/resources/',
    'project_details': '%(hostname)s/api/2/project/%(project)s/?details',
    'resource_details': '%(hostname)s/api/2/project/%(project)s/resource/%(resource)s/',
    'release_details': '%(hostname)s/api/2/project/%(project)s/release/%(release)s/',
    'pull_file': '%(hostname)s/api/2/project/%(project)s/resource/%(resource)s/translation/%(language)s/?file',
    'pull_reviewed_file': '%(hostname)s/api/2/project/%(project)s/resource/%(resource)s/translation/%(language)s/?file&mode=reviewed',
    'pull_translator_file': '%(hostname)s/api/2/project/%(project)s/resource/%(resource)s/translation/%(language)s/?file&mode=translated',
    'pull_developer_file': '%(hostname)s/api/2/project/%(project)s/resource/%(resource)s/translation/%(language)s/?file&mode=default',
    'resource_stats': '%(hostname)s/api/2/project/%(project)s/resource/%(resource)s/stats/',
    'create_resource': '%(hostname)s/api/2/project/%(project)s/resources/',
    'push_source': '%(hostname)s/api/2/project/%(project)s/resource/%(resource)s/content/',
    'push_translation': '%(hostname)s/api/2/project/%(project)s/resource/%(resource)s/translation/%(language)s/',
    'delete_translation': '%(hostname)s/api/2/project/%(project)s/resource/%(resource)s/translation/%(language)s/',
    'formats': '%(hostname)s/api/2/formats/',
    'delete_resource': '%(hostname)s/api/2/project/%(project)s/resource/%(resource)s/',
}


