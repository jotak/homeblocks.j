## Homeblocks (Java)

Yet another rewrite of homeblocks.
I wanted to learn about some of the new stuff Java got in the last decade.
I'm also feeling that maintenance will be easier than in Kotlin (we'll see).

### Build

```bash
mvn clean package
```

### Deploy

- Copy `target/homeblocks-0.0.2-fat.jar` to the host, as well as `public/`.

- Needs a `server.json` file in the working directory, such as:

```json
{
  "tlsPort": 443,
  "clearPort": 80,
  "tlsCertPath": "/path/to/fullchain.pem",
  "tlsKeyPath": "/path/to/privkey.pem"
}
```

If `tlsCertPath` or `tlsKeyPath` are omitted, starts without TLS.

- Also needs oauth2 info in `oauth/`, such as:

`oauth/github.json`
```json
{
  "type": "github",
  "shortName": "gh",
  "displayName": "GitHub",
  "redirectURI": "https://homeblocks.net/oauthclbk-gh",
  "config": {
    "clientID": "xxx",
    "clientSecret": "xxx",
    "site": "https://github.com/login",
    "tokenPath": "/oauth/access_token",
    "authorizationPath": "/oauth/authorize",
    "userInfoPath": "https://api.github.com/user",
    "headers": {
      "User-Agent": "jotak-homeblocks"
    }
  }
}
```

- To not run as root on ports 80/443, needs some host config; as described here: https://superuser.com/questions/710253/allow-non-root-process-to-bind-to-port-80-and-443.

For instance, if the host isn't shared with multiple users, it is good enough to change the unprivileged port config by editing `/etc/sysctl.conf`:

```
net.ipv4.ip_unprivileged_port_start=80
```

Make also sure the key & certificates are readable for the user.

### Run

```bash
java -classpath java/homeblocks-0.0.2-fat.jar net.homeblocks.MainVerticle
```
