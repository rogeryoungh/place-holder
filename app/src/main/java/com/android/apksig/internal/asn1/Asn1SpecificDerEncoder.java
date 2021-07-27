/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.apksig.internal.asn1;

import com.android.apksig.internal.asn1.ber.BerEncoding;
import com.android.apksig.internal.pkcs7.AlgorithmIdentifier;
import com.android.apksig.internal.pkcs7.ContentInfo;
import com.android.apksig.internal.pkcs7.EncapsulatedContentInfo;
import com.android.apksig.internal.pkcs7.IssuerAndSerialNumber;
import com.android.apksig.internal.pkcs7.SignedData;
import com.android.apksig.internal.pkcs7.SignerIdentifier;
import com.android.apksig.internal.pkcs7.SignerInfo;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Encoder of ASN.1 structures into DER-encoded form.
 *
 * <p>Structure is described to the encoder by providing a class annotated with {@link Asn1Class},
 * containing fields annotated with {@link Asn1Field}.
 */
public final class Asn1SpecificDerEncoder {
    private Asn1SpecificDerEncoder() {}

    /**
     * Returns the DER-encoded form of the provided ASN.1 structure.
     *
     * @param container container to be encoded. The container's class must meet the following
     *        requirements:
     *        <ul>
     *        <li>The class must be annotated with {@link Asn1Class}.</li>
     *        <li>Member fields of the class which are to be encoded must be annotated with
     *            {@link Asn1Field} and be public.</li>
     *        </ul>
     *
     * @throws Asn1EncodingException if the input could not be encoded
     */
    public static byte[] encode(Object container) throws Asn1EncodingException {
        Class<?> containerClass = container.getClass();
        if(containerClass == AlgorithmIdentifier.class) {
            return AlgorithmIdentifierEncoder((AlgorithmIdentifier) container);
        }
        return SignerInfoEncoder((SignerInfo) container);
    }

    private static byte[] toChoice(SignerIdentifier container) {
        if(container.issuerAndSerialNumber != null)
            return IssuerAndSerialNumberEncoder(container.issuerAndSerialNumber);

        ByteBuffer buf = container.subjectKeyIdentifier;
        byte[] value = new byte[buf.remaining()];
        buf.slice().get(value);
        value = createTag(
                BerEncoding.TAG_CLASS_UNIVERSAL,
                false,
                BerEncoding.getTagNumber(Asn1Type.OCTET_STRING),
                value);

        value[0] = BerEncoding.setTagNumber(value[0], 0);
        value[0] = BerEncoding.setTagClass(value[0], 0);
        return value;
    }

    private static byte[] toSetOf(Collection<?> values) throws Asn1EncodingException {
        return toSequenceOrSetOf(values);
    }

    private static byte[] toSequenceOrSetOf(Collection<?> values)
            throws Asn1EncodingException {
        List<byte[]> serializedValues = new ArrayList<>(values.size());
        for (Object value : values) {
            serializedValues.add(JavaToDerConverter.toDer(value, Asn1Type.ANY));
        }
        int tagNumber;
        if (serializedValues.size() > 1) {
            Collections.sort(serializedValues, ByteArrayLexicographicComparator.INSTANCE);
        }
        tagNumber = BerEncoding.TAG_NUMBER_SET;
        return createTag(
                BerEncoding.TAG_CLASS_UNIVERSAL, true, tagNumber,
                serializedValues.toArray(new byte[0][]));
    }

    /**
     * Compares two bytes arrays based on their lexicographic order. Corresponding elements of the
     * two arrays are compared in ascending order. Elements at out of range indices are assumed to
     * be smaller than the smallest possible value for an element.
     */
    private static class ByteArrayLexicographicComparator implements Comparator<byte[]> {
            private static final ByteArrayLexicographicComparator INSTANCE =
                    new ByteArrayLexicographicComparator();

            @Override
            public int compare(byte[] arr1, byte[] arr2) {
                int commonLength = Math.min(arr1.length, arr2.length);
                for (int i = 0; i < commonLength; i++) {
                    int diff = (arr1[i] & 0xff) - (arr2[i] & 0xff);
                    if (diff != 0) {
                        return diff;
                    }
                }
                return arr1.length - arr2.length;
            }
    }

    private static byte[] toInteger(int value) {
        return toInteger((long) value);
    }

    private static byte[] toInteger(long value) {
        return toInteger(BigInteger.valueOf(value));
    }

    private static byte[] toInteger(BigInteger value) {
        return createTag(
                BerEncoding.TAG_CLASS_UNIVERSAL, false, BerEncoding.TAG_NUMBER_INTEGER,
                value.toByteArray());
    }

    private static byte[] toOid(String oid) throws Asn1EncodingException {
        ByteArrayOutputStream encodedValue = new ByteArrayOutputStream();
        String[] nodes = oid.split("\\.");
        if (nodes.length < 2) {
            throw new Asn1EncodingException(
                    "OBJECT IDENTIFIER must contain at least two nodes: " + oid);
        }
        int firstNode;
        try {
            firstNode = Integer.parseInt(nodes[0]);
        } catch (NumberFormatException e) {
            throw new Asn1EncodingException("Node #1 not numeric: " + nodes[0]);
        }
        if ((firstNode > 6) || (firstNode < 0)) {
            throw new Asn1EncodingException("Invalid value for node #1: " + firstNode);
        }

        int secondNode;
        try {
            secondNode = Integer.parseInt(nodes[1]);
        } catch (NumberFormatException e) {
            throw new Asn1EncodingException("Node #2 not numeric: " + nodes[1]);
        }
        if ((secondNode >= 40) || (secondNode < 0)) {
            throw new Asn1EncodingException("Invalid value for node #2: " + secondNode);
        }
        int firstByte = firstNode * 40 + secondNode;
        if (firstByte > 0xff) {
            throw new Asn1EncodingException(
                    "First two nodes out of range: " + firstNode + "." + secondNode);
        }

        encodedValue.write(firstByte);
        for (int i = 2; i < nodes.length; i++) {
            String nodeString = nodes[i];
            int node;
            try {
                node = Integer.parseInt(nodeString);
            } catch (NumberFormatException e) {
                throw new Asn1EncodingException("Node #" + (i + 1) + " not numeric: " + nodeString);
            }
            if (node < 0) {
                throw new Asn1EncodingException("Invalid value for node #" + (i + 1) + ": " + node);
            }
            if (node <= 0x7f) {
                encodedValue.write(node);
                continue;
            }
            if (node < 1 << 14) {
                encodedValue.write(0x80 | (node >> 7));
                encodedValue.write(node & 0x7f);
                continue;
            }
            if (node < 1 << 21) {
                encodedValue.write(0x80 | (node >> 14));
                encodedValue.write(0x80 | ((node >> 7) & 0x7f));
                encodedValue.write(node & 0x7f);
                continue;
            }
            throw new Asn1EncodingException("Node #" + (i + 1) + " too large: " + node);
        }

        return createTag(
                BerEncoding.TAG_CLASS_UNIVERSAL, false, BerEncoding.TAG_NUMBER_OBJECT_IDENTIFIER,
                encodedValue.toByteArray());
    }

    private static byte[] createTag(
            int tagClass, boolean constructed, int tagNumber, byte[]... contents) {
        if (tagNumber >= 0x1f) {
            throw new IllegalArgumentException("High tag numbers not supported: " + tagNumber);
        }
        // tag class & number fit into the first byte
        byte firstIdentifierByte =
                (byte) ((tagClass << 6) | (constructed ? 1 << 5 : 0) | tagNumber);

        int contentsLength = 0;
        for (byte[] c : contents) {
            contentsLength += c.length;
        }
        int contentsPosInResult;
        byte[] result;
        if (contentsLength < 0x80) {
            // Length fits into one byte
            contentsPosInResult = 2;
            result = new byte[contentsPosInResult + contentsLength];
            result[0] = firstIdentifierByte;
            result[1] = (byte) contentsLength;
        } else {
            // Length is represented as multiple bytes
            // The low 7 bits of the first byte represent the number of length bytes (following the
            // first byte) in which the length is in big-endian base-256 form
            if (contentsLength <= 0xff) {
                contentsPosInResult = 3;
                result = new byte[contentsPosInResult + contentsLength];
                result[1] = (byte) 0x81; // 1 length byte
                result[2] = (byte) contentsLength;
            } else if (contentsLength <= 0xffff) {
                contentsPosInResult = 4;
                result = new byte[contentsPosInResult + contentsLength];
                result[1] = (byte) 0x82; // 2 length bytes
                result[2] = (byte) (contentsLength >> 8);
                result[3] = (byte) (contentsLength & 0xff);
            } else if (contentsLength <= 0xffffff) {
                contentsPosInResult = 5;
                result = new byte[contentsPosInResult + contentsLength];
                result[1] = (byte) 0x83; // 3 length bytes
                result[2] = (byte) (contentsLength >> 16);
                result[3] = (byte) ((contentsLength >> 8) & 0xff);
                result[4] = (byte) (contentsLength & 0xff);
            } else {
                contentsPosInResult = 6;
                result = new byte[contentsPosInResult + contentsLength];
                result[1] = (byte) 0x84; // 4 length bytes
                result[2] = (byte) (contentsLength >> 24);
                result[3] = (byte) ((contentsLength >> 16) & 0xff);
                result[4] = (byte) ((contentsLength >> 8) & 0xff);
                result[5] = (byte) (contentsLength & 0xff);
            }
            result[0] = firstIdentifierByte;
        }
        for (byte[] c : contents) {
            System.arraycopy(c, 0, result, contentsPosInResult, c.length);
            contentsPosInResult += c.length;
        }
        return result;
    }

    private static final class JavaToDerConverter {
        private JavaToDerConverter() {}

        public static byte[] toDer(Object source, Asn1Type targetType)
                throws Asn1EncodingException {
            Class<?> sourceType = source.getClass();
            if (Asn1OpaqueObject.class.equals(sourceType)) {
                ByteBuffer buf = ((Asn1OpaqueObject) source).getEncoded();
                byte[] result = new byte[buf.remaining()];
                buf.get(result);
                return result;
            }

            if ((targetType == null) || (targetType == Asn1Type.ANY)) {
                return encode(source);
            }

            return IssuerAndSerialNumberEncoder((IssuerAndSerialNumber) source);
        }
    }

    public static byte[] Asn1OpaqueObjectEncoder(Asn1OpaqueObject object) {
        ByteBuffer buf = object.getEncoded();
        byte[] result = new byte[buf.remaining()];
        buf.get(result);
        return result;
    }

    public static byte[] ContentInfoEncoder(ContentInfo object) throws Asn1EncodingException {
        List<byte[]> serializedFields = new ArrayList<>(2);
        byte[] serializedField;

        serializedField = toOid(object.contentType);
        serializedFields.add(serializedField);

        byte[] bytes = Asn1OpaqueObjectEncoder(object.content);
        serializedField = createTag(2, true, 0, bytes);
        serializedFields.add(serializedField);

        return createTag(
                BerEncoding.TAG_CLASS_UNIVERSAL, true, BerEncoding.TAG_NUMBER_SEQUENCE,
                serializedFields.toArray(new byte[0][]));
    }

    public static byte[] SignedDataEncoder(SignedData object) throws Asn1EncodingException {

        List<byte[]> serializedFields = new ArrayList<>(6);
        byte[] serializedField;


        serializedField = toInteger(object.version);
        serializedFields.add(serializedField);


        serializedField = toSetOf(object.digestAlgorithms);
        serializedFields.add(serializedField);


        serializedField = EncapsulatedContentInfoEncoder(object.encapContentInfo);
        serializedFields.add(serializedField);


        if(object.certificates != null) {
            serializedField = toSetOf(object.certificates);
            serializedField[0] = BerEncoding.setTagNumber(serializedField[0], 0);
            serializedField[0] = BerEncoding.setTagClass(serializedField[0], 2);
            serializedFields.add(serializedField);
        }

        if(object.crls != null) {
            serializedField = toSetOf(object.crls);
            serializedField[0] = BerEncoding.setTagNumber(serializedField[0], 1);
            serializedField[0] = BerEncoding.setTagClass(serializedField[0], 2);
            serializedFields.add(serializedField);
        }

        serializedField = toSetOf(object.signerInfos);
        serializedFields.add(serializedField);

        return createTag(
                BerEncoding.TAG_CLASS_UNIVERSAL, true, BerEncoding.TAG_NUMBER_SEQUENCE,
                serializedFields.toArray(new byte[0][]));
    }

    public static byte[] EncapsulatedContentInfoEncoder(EncapsulatedContentInfo object) throws Asn1EncodingException {

        List<byte[]> serializedFields = new ArrayList<>(2);
        byte[] serializedField;

        serializedField = toOid(object.contentType);
        serializedFields.add(serializedField);


        if (object.content != null) {
            ByteBuffer buf = object.content;
            byte[] value = new byte[buf.remaining()];
            buf.slice().get(value);
            serializedField = createTag(
                    BerEncoding.TAG_CLASS_UNIVERSAL,
                    false,
                    BerEncoding.getTagNumber(Asn1Type.OCTET_STRING),
                    value);
            serializedFields.add(serializedField);
        }

        return createTag(
                BerEncoding.TAG_CLASS_UNIVERSAL, true, BerEncoding.TAG_NUMBER_SEQUENCE,
                serializedFields.toArray(new byte[0][]));
    }

    public static byte[] AlgorithmIdentifierEncoder(AlgorithmIdentifier object) throws Asn1EncodingException {

        List<byte[]> serializedFields = new ArrayList<>(2);
        byte[] serializedField;

        serializedField = toOid(object.algorithm);
        serializedFields.add(serializedField);

        if(object.parameters != null) {
            serializedField = Asn1OpaqueObjectEncoder(object.parameters);
            serializedFields.add(serializedField);
        }


        return createTag(
                BerEncoding.TAG_CLASS_UNIVERSAL, true, BerEncoding.TAG_NUMBER_SEQUENCE,
                serializedFields.toArray(new byte[0][]));
    }

    public static byte[] IssuerAndSerialNumberEncoder(IssuerAndSerialNumber object) {
        List<byte[]> serializedFields = new ArrayList<>(2);
        byte[] serializedField;

        serializedField = Asn1OpaqueObjectEncoder(object.issuer);
        serializedFields.add(serializedField);

        serializedField = toInteger(object.certificateSerialNumber);
        serializedFields.add(serializedField);

        return createTag(
                BerEncoding.TAG_CLASS_UNIVERSAL, true, BerEncoding.TAG_NUMBER_SEQUENCE,
                serializedFields.toArray(new byte[0][]));
    }

    public static byte[] SignerInfoEncoder(SignerInfo object) throws Asn1EncodingException {
        List<byte[]> serializedFields = new ArrayList<>(7);
        byte[] serializedField;


        serializedField = toInteger(object.version);
        serializedFields.add(serializedField);


        serializedField = toChoice(object.sid);
        serializedFields.add(serializedField);

        serializedField = AlgorithmIdentifierEncoder(object.digestAlgorithm);
        serializedFields.add(serializedField);

        if(object.signedAttrs != null) {
            // WHY signedAttrs is SET_OF ???
            serializedField = toSetOf((Collection<?>) object.signedAttrs);
            serializedField[0] = BerEncoding.setTagNumber(serializedField[0], 0);
            serializedField[0] = BerEncoding.setTagClass(serializedField[0], BerEncoding.TAG_CLASS_UNIVERSAL);
            serializedFields.add(serializedField);
        }

        serializedField = AlgorithmIdentifierEncoder(object.signatureAlgorithm);
        serializedFields.add(serializedField);

        ByteBuffer buf = object.signature;
        byte[] value = new byte[buf.remaining()];
        buf.slice().get(value);
        serializedField = createTag(
                BerEncoding.TAG_CLASS_UNIVERSAL,
                false,
                BerEncoding.getTagNumber(Asn1Type.OCTET_STRING),
                value);
        serializedFields.add(serializedField);


        if(object.unsignedAttrs != null) {
            serializedField = toSetOf((Collection<?>) object.unsignedAttrs);
            serializedField[0] = BerEncoding.setTagNumber(serializedField[0], 1);
            serializedField[0] = BerEncoding.setTagClass(serializedField[0], BerEncoding.TAG_CLASS_UNIVERSAL);
            serializedFields.add(serializedField);
        }

        return createTag(
                BerEncoding.TAG_CLASS_UNIVERSAL, true, BerEncoding.TAG_NUMBER_SEQUENCE,
                serializedFields.toArray(new byte[0][]));
    }
}
