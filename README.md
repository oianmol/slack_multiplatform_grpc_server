# Slack Kotlin gRPC server for the multiplatform client!

#### Initialize ECDSA keys

Use the provided utility program to generate an ECDSA key pair:
```shell
$ ./gradlew :tools:installDist
$ ./tools/build/install/tools/bin/ecdsa-key-pair-generator \
> --ecdsa_public_key_path=android-key-path/sender_verification_key.dat \
> --ecdsa_private_key_path=src/main/resources/ecdsa/sender_signing_key.dat
```


#### Initialize TLS keys

The demo supports both RSA-based and Elliptic Curve (EC)-based TLS keys.
To generate EC-based TLS keys:

1. Add the hostname of the server in which the demo application server will be run in the
   `[test_sans]` section of the [`resources/tls/init_tls.cnf`](resources/tls/init_tls.cnf) file.
   Note that this file already contains the hostnames that are typically used for local development
   (`127.0.0.1` for `localhost`, and `10.0.2.2` for connecting to `localhost` from an emulated Android
   device [more info](https://developer.android.com/studio/run/emulator-networking.html)).

1. Generate TLS keys:
```shell
$ cd src/main/resources/tls
$ openssl req -x509 -days 365 -nodes -newkey ec:<(openssl ecparam -name prime256v1) -keyout tls_tmp.key -out tls.crt -config init_tls.cnf
$ openssl pkcs8 -topk8 -nocrypt -in tls_tmp.key -out tls.key
$ rm tls_tmp.key
```

Then Copy tls.crt ../../android/src/main/res/raw

See the instructions in [`resources/tls/init_tls.cnf`](resources/tls/init_tls.cnf) to generate RSA-based TLS keys.