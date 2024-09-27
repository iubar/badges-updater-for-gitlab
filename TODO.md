Jenkins

- Per recuperare un elenco dei job: http://192.168.0.113:8080/api/json?tree=jobs[name,color, description]
- Per modificare la descrizione del job via api: https://stackoverflow.com/questions/25427622/changing-jenkins-build-name-description-through-api-in-java ) (il progetto andrà quindi rinominato)

Custom badges

Per implementare nuovi badge (ad esenpio numero degli issue aperti), occorre scrivere script python da utilizzare su server http che traduca richiesta GET in immagine png
Vedi come implementare server http in python:

- https://stackabuse.com/serving-files-with-pythons-simplehttpserver-module/
  Esempio di implementazione (stessa implementazione in versioni diverse, sempre più ottimizzate):
- https://gist.github.com/elbosso/4f3bb0fb95dd1dc499cd46db422900bf
- https://gist.github.com/elbosso/e160341d5e00e0a726ff8725af560373
- https://gist.github.com/elbosso/cbce113805735478e926c28a9079cede
- https://gist.github.com/elbosso/6637702612991bb454d205d936dd04dc
  Perchè Python ? Perchè ho la libreria anybadge https://github.com/jongracecox/anybadge
  In alternativa potrei usare il seguente servizio web
- https://shields.io/category/issue-tracking
- https://shields.io/endpoint
  Un esempio è quello proposto qui: https://gitlab.com/gitlab-org/gitlab-foss/issues/46188#note_103801317

> > > TODO: RINOMINARE PROGETTO IN iubar-gitlab-maintenance <<<

TODO:
leggere Config.UPDATE_BADGES da file Config
leggere Config.DELETE_PIPELINE da file Config
leggere Config.UPDATE_WEBHOOKS da file Config
