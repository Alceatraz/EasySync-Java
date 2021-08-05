# EaSync

A simple tool for folder single direction sync over sftp (client to remote). Stupid fast and stupid easy.

You need a simple config file `config.properties` (should found at working director, just beside the jar executable)

```properties
easy-sync.server.host=YOUR-SERVER-HOSTNAME
easy-sync.server.port=YOUR-SERVER-SSH-PORT
easy-sync.username=YOUR-SSH-LOGIN-NAME
easy-sync.key.path=YOUR-SSH-KEY-FILE(local file)
easy-sync.key.hash=YOUR-SSH-KEY-HASH
easy-sync.path.client=LOCAL-FOLDER
easy-sync.path.server=REMOTE-FOLDER
easy-sync.mode.delete=EXECUT-REMOTE-DELETE
```


Caution: DELETE mode will delete remote file if you delete local file, So if you careless, You will lose it!
