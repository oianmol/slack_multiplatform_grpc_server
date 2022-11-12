/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.capillary.tools;

import com.google.crypto.tink.*;
import com.google.crypto.tink.proto.KeyTemplate;
import com.google.crypto.tink.signature.SignatureConfig;
import com.google.crypto.tink.signature.SignatureKeyTemplates;
import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import static com.google.crypto.tink.signature.SignatureKeyTemplates.ECDSA_P256;

/**
 * A utility program to generate ECDSA public/private key pairs.
 *
 * <p>To generate keys, run the program with the following command line arguments:
 * <ul>
 * <li>ecdsa_public_key_path: The path to store the generated public key.
 * <li>ecdsa_private_key_path: The path to store the generated private key.
 * </ul>
 */
public final class EcdsaKeyPairGenerator {

  private static final String ECDSA_PUBLIC_KEY_PATH_OPTION = "ecdsa_public_key_path";
  private static final String ECDSA_PRIVATE_KEY_PATH_OPTION = "ecdsa_private_key_path";

  /**
   * Parses the command line arguments and generates an ECDSA private/private key pair.
   */
  public static void main(String[] args) throws Exception {
    Config.register(SignatureConfig.LATEST);
    CommandLine commandLine = generateCommandLine(args);
    generateEcdsaKeyPair(
        new File(commandLine.getOptionValue(ECDSA_PUBLIC_KEY_PATH_OPTION)),
        new File(commandLine.getOptionValue(ECDSA_PRIVATE_KEY_PATH_OPTION)));
  }

  private static CommandLine generateCommandLine(String[] commandLineArguments)
      throws ParseException {
    Option ecdsaPublicKeyPath = Option.builder()
        .longOpt(ECDSA_PUBLIC_KEY_PATH_OPTION)
        .desc("The path to store ECDSA public key.")
        .hasArg()
        .required()
        .build();
    Option ecdsaPrivateKeyPath = Option.builder()
        .longOpt(ECDSA_PRIVATE_KEY_PATH_OPTION)
        .desc("The path to store ECDSA private key.")
        .hasArg()
        .required()
        .build();

    Options options = new Options();
    options.addOption(ecdsaPublicKeyPath);
    options.addOption(ecdsaPrivateKeyPath);

    CommandLineParser cmdLineParser = new DefaultParser();
    return cmdLineParser.parse(options, commandLineArguments);
  }

  private static void generateEcdsaKeyPair(File publicKeyFile, File privatekeyFile)
      throws GeneralSecurityException, IOException {
    KeysetHandle privateKeyHandle = KeysetHandle.generateNew(KeyTemplates.get("ECDSA_P256"));
    CleartextKeysetHandle.write(privateKeyHandle, BinaryKeysetWriter.withFile(privatekeyFile));
    KeysetHandle publicKeyHandle = privateKeyHandle.getPublicKeysetHandle();
    CleartextKeysetHandle.write(publicKeyHandle, BinaryKeysetWriter.withFile(publicKeyFile));
  }
}
