
a) Aggiungere quality gate badge: 
( vedi http://192.168.0.117:9000/api/badges/gate?key=core:php-iubar-builder )

b) utilizzare i parametri %{project_path} e %{default_branch} nella costruzione del link al pipeline badge: https://example.gitlab.com/%{project_path}/badges/%{default_branch}/badge.svg (vedi https://gitlab.iubar.it/help/user/project/badges). Nota che in Gitlab, il valore di %{project_path} è scollegato dal valore di <project_name> anche se di default %{project_path} assume il seguente formato <nome_gruppo>/<project_name>.

c) Aggiungere chiamata api per cancellare le vecchie pipeline. Vedere https://docs.gitlab.com/ee/api/pipelines.html#cancel-a-pipelines-jobs

d) Ampliare il raggio d'azione del progetto al fine di modificare la descrizione dei job di Jenkins
( per recuperare un elenco dei job: http://192.168.0.113:8080/api/json?tree=jobs[name,color, description] )
( per modificare la descrizione del job via api: https://stackoverflow.com/questions/25427622/changing-jenkins-build-name-description-through-api-in-java ) (il progetto andrà quindi rinominato)

