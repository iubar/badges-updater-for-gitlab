

b) Aggiungere chiamata api per cancellare le vecchie pipeline. Vedere https://docs.gitlab.com/ee/api/pipelines.html#cancel-a-pipelines-jobs

Ampliare il raggio d'azione del progetto al fine di modificare la descrizione dei job di Jenkins
( per recuperare un elenco dei job: http://192.168.0.113:8080/api/json?tree=jobs[name,color, description] )
( per modificare la descrizione del job via api: https://stackoverflow.com/questions/25427622/changing-jenkins-build-name-description-through-api-in-java ) (il progetto andrà quindi rinominato)

