./gradlew :tools:installDist || "TODO add a strategy"
./tools/build/install/tools/bin/ecdsa-key-pair-generator --ecdsa_public_key_path=android-key-path/sender_verification_key.dat --ecdsa_private_key_path=src/main/resources/ecdsa/sender_signing_key.dat

cd src/main/resources/tls
openssl req -x509 -days 365 -nodes -newkey ec:<(openssl ecparam -name prime256v1) -keyout tls_tmp.key -out tls.crt -config init_tls.cnf
openssl pkcs8 -topk8 -nocrypt -in tls_tmp.key -out tls.key
rm tls_tmp.key