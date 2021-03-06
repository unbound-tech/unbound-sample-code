import com.dyadicsec.cryptoki.*;

public class RSAWrapUnwrap
{
  /***************
   *
   * @param args - if default user password is non-empty, provide it as first arg
   * @throws Exception
   */
  public static void main(String[] args) throws Exception
  {
    char[] pwd = null;
    if (args.length > 0) pwd = args[0].toCharArray();

    int slotId = 0;
    Library.C_Initialize();

    // Open PKCS#11 session
    System.out.println("Open Session and login with default user");
    CK_SESSION_HANDLE session = Library.C_OpenSession(slotId, CK.CKF_RW_SESSION | CK.CKF_SERIAL_SESSION);
    Library.C_Login(session, CK.CKU_USER, pwd); // Optional if password is null

    // Generate HMAC key
    System.out.println("Generate HMAC key");
    int hmacKey = Library.C_GenerateKey(session, new CK_MECHANISM(CK.CKM_GENERIC_SECRET_KEY_GEN),
      new CK_ATTRIBUTE[]
      {
        new CK_ATTRIBUTE(CK.CKA_TOKEN, false),
        new CK_ATTRIBUTE(CK.CKA_CLASS, CK.CKO_SECRET_KEY),
        new CK_ATTRIBUTE(CK.CKA_KEY_TYPE, CK.CKK_GENERIC_SECRET),
        new CK_ATTRIBUTE(CK.CKA_VALUE_LEN, 16), // HMAC 128 bit
      });

    // Generate RSA key pair
    System.out.println("Generate RSA key pair");
    int[] keyHandles = Library.C_GenerateKeyPair(session,
      new CK_MECHANISM(CK.CKM_RSA_PKCS_KEY_PAIR_GEN), // RSA generation mechanism
      new CK_ATTRIBUTE[]
      {
        new CK_ATTRIBUTE(CK.CKA_TOKEN, false),
        new CK_ATTRIBUTE(CK.CKA_MODULUS_BITS, 2048), // RSA key size
      },
      new CK_ATTRIBUTE[]
      {
        new CK_ATTRIBUTE(CK.CKA_TOKEN, true),
      });
    int pubKey = keyHandles[0];
    int prvKey = keyHandles[1];

    // Define RSA padding params
    System.out.println("Define RSA padding params");
    CK_RSA_PKCS_OAEP_PARAMS oaepParams = new CK_RSA_PKCS_OAEP_PARAMS();
    oaepParams.hashAlg = CK.CKM_SHA256;
    oaepParams.mgf = CK.CKG_MGF1_SHA256;
    oaepParams.source = CK.CKZ_DATA_SPECIFIED;
    oaepParams.pSourceData = "Label".getBytes("UTF-8");
    CK_MECHANISM mech = new CK_MECHANISM(CK.CKM_RSA_PKCS_OAEP, oaepParams);

    // Wrap HMAC key with RSA public
    System.out.println("Wrap HMAC key with RSA public");
    byte[] wrapped = Library.C_WrapKey(session, mech, pubKey, hmacKey);
    Library.C_DestroyObject(session, hmacKey);

    // Unwrap HMAC key with RSA private
    System.out.println("Unwrap HMAC key with RSA private");
    int unwrappedHmacKey = Library.C_UnwrapKey(session, mech, prvKey, wrapped,
      new CK_ATTRIBUTE[]
      {
        new CK_ATTRIBUTE(CK.CKA_TOKEN, false),
        new CK_ATTRIBUTE(CK.CKA_CLASS, CK.CKO_SECRET_KEY),
        new CK_ATTRIBUTE(CK.CKA_KEY_TYPE, CK.CKK_GENERIC_SECRET)
      });

    // Close PKCS#11 session
    System.out.println("Close PKCS#11 session");
    Library.C_CloseSession(session);
    Library.C_Finalize();
  }

}
