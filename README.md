# EaSync

A simple tool for folder single direction sync over sftp (client to remote). Stupid fast and stupid easy.

You need a simple config file `config.properties` (should found at working director, just beside the jar executable)

```properties
easy-sync.server.host=1.2.3.4      # YOUR-SERVER-HOSTNAME(string)
easy-sync.server.port=12345        # YOUR-SERVER-SSH-PORT(int)
easy-sync.username=foobar          # YOUR-SSH-LOGIN-NAME(string)
easy-sync.key.path=/my-ssh/backup  # YOUR-SSH-KEY-FILE(string,file path)
easy-sync.key.hash=SHA256:00000000 # YOUR-SSH-KEY-HASH(string)
easy-sync.path.client=/tank/client # LOCAL-FOLDER(string,file path)
easy-sync.path.server=/tank/server # REMOTE-FOLDER(string,file path)
easy-sync.mode.delete=true         # EXECUT-REMOTE-DELETE(true/false)
```


Caution: DELETE mode will delete remote file if you delete local file, So if you careless, You will lose it!
