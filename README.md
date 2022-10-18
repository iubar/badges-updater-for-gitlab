[![Build Status](https://app.travis-ci.com/iubar/badges-updater-for-gitlab.svg?branch=master)](https://app.travis-ci.com/github/iubar/badges-updater-for-gitlab)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/105ac6deae804246b20d140f976cd232)](https://www.codacy.com/gh/iubar/badges-updater-for-gitlab/dashboard)

# Badges updater for GitLab

The application inserts some Gitlab and SonarCube badges inside every projects stored on the GitLab server and delete the old pipelines

### Configuration

See the config file "config.properties.dist" on the path "src\main\resources".

```sh
sonar.host = http://127.0.0.1
gitlab.host = http://gitlab.example.com
gitlab.token = your_token
```

### Run from

- https://travis-ci.com/iubar/badges-updater-for-gitlab

### Riferimenti

- https://docs.gitlab.com/ee/api/
- https://docs.gitlab.com/ee/api/project_badges.html

The gitlab's builtin badges are two, see the source code at https://gitlab.com/gitlab-org/gitlab-foss/tree/master/lib/gitlab/badge

For example:

- https://gitlab.com/iubar/hello/badges/master/build.svg
- https://gitlab.com/iubar/hello/badges/master/coverage.svg

### Other related projects

- https://github.com/m4tthumphrey/php-gitlab-api (a PHP wrapper to be used with Gitlab's Api)
