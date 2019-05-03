[![Build Status](https://travis-ci.org/iubar/badges-updater-for-gitlab.svg?branch=master)](https://travis-ci.org/iubar/badges-updater-for-gitlab)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/2781d3e900d042d792c472621d8e7831)](https://www.codacy.com/app/Iubar/badges-updater-for-gitlab?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=iubar/badges-updater-for-gitlab&amp;utm_campaign=Badge_Grade)

# Badges updater for GitLab

The application inserts some Gitlab and SonarCube badges inside every projects stored on the GitLab server and delete the old pipelines

### Configuration

See the config file "config.properties.dist" on the path "src\main\resources".
```sh
  sonar.host = http://127.0.0.1
  gitlab.host = http://gitlab.example.com
  gitlab.token = your_token
```

### Run

- https://travis-ci.org/iubar/badges-updater-for-gitlab

# Riferimenti

 - https://docs.gitlab.com/ee/api/project_badges.html
 - https://docs.gitlab.com/ee/user/project/pipelines/settings.html#pipeline-badges
 - https://docs.gitlab.com/ee/api/pipelines.html#cancel-a-pipelines-jobs
 - https://en.wikipedia.org/wiki/List_of_HTTP_status_codes

# Other related projects

 - https://github.com/m4tthumphrey/php-gitlab-api
