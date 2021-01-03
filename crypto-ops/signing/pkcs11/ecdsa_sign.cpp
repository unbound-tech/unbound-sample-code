#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "dy_pkcs11.h"

void halt(CK_RV rv)
{
	printf("Error %08lx\n", rv);
	exit(-1);
}

int main(int argc, char *argv[])
{
	CK_SLOT_ID slot_id = 0; // default slot ID
	CK_SESSION_HANDLE hSession = 0;
	CK_RV rv;
	const char data_to_sign[] = "data to sign";

	rv = C_Initialize(NULL);
	if (rv != CKR_OK)
		halt(rv);
	// Open PKCS#11 session
	rv = C_OpenSession(slot_id, CKF_SERIAL_SESSION | CKF_RW_SESSION, 0, 0, &hSession);
	if (rv != CKR_OK)
		halt(rv);

	char password[] = ""; // ------ set your password here -------
	rv = C_Login(hSession, CKU_USER, CK_CHAR_PTR(password), CK_ULONG(strlen(password)));
	if (rv != CKR_OK)
		halt(rv);

	// RSA generation mechanism
	CK_MECHANISM ecdsa_gen = {CKM_EC_KEY_PAIR_GEN, NULL, 0};

	// ECDSA curve OID
	CK_BYTE p256_curve[] = {0x06, 0x08, 0x2a, 0x86, 0x48, 0xce, 0x3d, 0x03, 0x01, 0x07};

	CK_BBOOL ck_false = CK_FALSE;
	CK_BBOOL ck_true = CK_TRUE;

	CK_ATTRIBUTE pub_template[] =
		{
			{CKA_TOKEN, &ck_false, sizeof(CK_BBOOL)},
			{CKA_EC_PARAMS, p256_curve, sizeof(p256_curve)},
		};

	CK_ATTRIBUTE prv_template[] =
		{
			{CKA_TOKEN, &ck_true, sizeof(CK_BBOOL)},
			{CKA_SIGN, &ck_true, sizeof(CK_BBOOL)},
		};

	// Generate key pair
	CK_OBJECT_HANDLE pub_key = 0, prv_key = 0;
	rv = C_GenerateKeyPair(hSession, &ecdsa_gen,
						   pub_template, sizeof(pub_template) / sizeof(CK_ATTRIBUTE),
						   prv_template, sizeof(prv_template) / sizeof(CK_ATTRIBUTE),
						   &pub_key, &prv_key);
	if (rv != CKR_OK)
		halt(rv);

	// Sign data
	CK_MECHANISM ecdsa_sign = {CKM_ECDSA_SHA256, NULL, 0};
	rv = C_SignInit(hSession, &ecdsa_sign, prv_key);
	if (rv != CKR_OK)
		halt(rv);

	CK_ULONG signature_len;
	rv = C_Sign(hSession, (CK_BYTE_PTR)data_to_sign, (CK_ULONG)sizeof(data_to_sign), NULL, &signature_len);
	if (rv != CKR_OK)
		halt(rv);

	CK_BYTE_PTR signature = (CK_BYTE_PTR)malloc(signature_len);
	rv = C_Sign(hSession, (CK_BYTE_PTR)data_to_sign, (CK_ULONG)sizeof(data_to_sign), signature, &signature_len);
	if (rv != CKR_OK)
		halt(rv);

	// Verify signature
	rv = C_VerifyInit(hSession, &ecdsa_sign, pub_key);
	if (rv != CKR_OK)
		halt(rv);

	rv = C_Verify(hSession, (CK_BYTE_PTR)data_to_sign, (CK_ULONG)sizeof(data_to_sign), signature, signature_len);
	if (rv != CKR_OK)
		halt(rv);

	// Close PKCS#11 session
	C_CloseSession(hSession);
	C_Finalize(NULL);
}