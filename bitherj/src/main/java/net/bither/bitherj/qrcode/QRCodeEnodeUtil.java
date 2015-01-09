/*
 * Copyright 2014 http://Bither.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.bither.bitherj.qrcode;


import net.bither.bitherj.core.Address;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.Tx;
import net.bither.bitherj.exception.AddressFormatException;
import net.bither.bitherj.utils.Base58;

import net.bither.bitherj.utils.Utils;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QRCodeEnodeUtil {
    private static final Logger log = LoggerFactory.getLogger(QRCodeEnodeUtil.class);

    private static final String QR_CODE_LETTER = "*";

    public static String getPublicKeyStrOfPrivateKey() {
        String content = "";
        List<Address> addresses = AddressManager.getInstance().getPrivKeyAddresses();
        for (int i = 0; i < addresses.size(); i++) {
            Address address = addresses.get(i);
            String pubStr = "";
            if (address.isFromXRandom()) {
                pubStr = QRCodeUtil.XRANDOM_FLAG;
            }
            pubStr = pubStr + Utils.bytesToHexString(address.getPubKey());
            content += pubStr;
            if (i < addresses.size() - 1) {
                content += QRCodeUtil.QR_CODE_SPLIT;
            }
        }
        content.toUpperCase(Locale.US);
        return content;
    }

    public static List<Address> formatPublicString(String content) {
        String[] strs = QRCodeUtil.splitString(content);
        ArrayList<Address> wallets = new ArrayList<Address>();
        for (String str : strs) {
            boolean isXRandom = false;
            if (str.indexOf(QRCodeUtil.XRANDOM_FLAG) == 0) {
                isXRandom = true;
                str = str.substring(1);
            }
            byte[] pub = Utils.hexStringToByteArray(str);
            String addString = Utils.toAddress(Utils.sha256hash160(pub));
            Address address = new Address(addString, pub, null, isXRandom);
            wallets.add(address);
        }
        return wallets;

    }

    private static QRCodeTxTransport fromSendRequestWithUnsignedTransaction(Tx tx, String addressCannotParsed) {
        QRCodeTxTransport qrCodeTransport = new QRCodeTxTransport();
        qrCodeTransport.setMyAddress(tx.getFromAddress());
        String toAddress = tx.getFirstOutAddress();
        if (Utils.isEmpty(toAddress)) {
            toAddress = addressCannotParsed;
        }
        qrCodeTransport.setToAddress(toAddress);
        qrCodeTransport.setTo(tx.amountSentToAddress(toAddress));
        qrCodeTransport.setFee(tx.getFee());
        List<String> hashList = new ArrayList<String>();
        for (byte[] h : tx.getUnsignedInHashes()) {
            hashList.add(Utils.bytesToHexString(h));
        }
        qrCodeTransport.setHashList(hashList);
        return qrCodeTransport;
    }

    public static String getPresignTxString(Tx tx, String address, String addressCannotParsed) {
        QRCodeTxTransport qrCodeTransport = fromSendRequestWithUnsignedTransaction(tx, addressCannotParsed);
        String preSignString = "";
        try {
            String changeStr = "";
            if (!Utils.isEmpty(address)) {
                long changeAmt = tx.amountSentToAddress(address);
                if (changeAmt != 0) {
                    changeStr = Base58.bas58ToHexWithAddress(address) + QRCodeUtil.QR_CODE_SPLIT + Long.toHexString(changeAmt)
                            .toLowerCase(Locale.US)
                            + QRCodeUtil.QR_CODE_SPLIT;
                }
            }
            preSignString = Base58.bas58ToHexWithAddress(qrCodeTransport.getMyAddress())
                    + QRCodeUtil.QR_CODE_SPLIT + changeStr
                    + Long.toHexString(qrCodeTransport.getFee())
                    .toLowerCase(Locale.US)
                    + QRCodeUtil.QR_CODE_SPLIT
                    + Base58.bas58ToHexWithAddress(qrCodeTransport.getToAddress())
                    + QRCodeUtil.QR_CODE_SPLIT
                    + Long.toHexString(qrCodeTransport.getTo())
                    .toLowerCase(Locale.US) + QRCodeUtil.QR_CODE_SPLIT;
            for (int i = 0; i < qrCodeTransport.getHashList().size(); i++) {
                String hash = qrCodeTransport.getHashList().get(i);
                if (i < qrCodeTransport.getHashList().size() - 1) {
                    preSignString = preSignString + hash + QRCodeUtil.QR_CODE_SPLIT;
                } else {
                    preSignString = preSignString + hash;
                }
            }
            preSignString.toUpperCase(Locale.US);
        } catch (AddressFormatException e) {
            e.printStackTrace();
        }

        return preSignString;
    }

    private static boolean isAddressHex(String str) {
        boolean isAddress = false;
        if (str.length() % 2 == 0) {
            try {
                String address = Base58.hexToBase58WithAddress(str);
                isAddress = Utils.validBicoinAddress(address);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return isAddress;
    }

    public static QRCodeTxTransport formatQRCodeTransport(String str) {
        try {

            String[] strArray = QRCodeUtil.splitString(str);
            boolean isAddress = isAddressHex(strArray[1]);
            if (isAddress) {
                return changeFormatQRCodeTransport(str);
            } else {
                return noChangeFormatQRCodeTransport(str);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    private static QRCodeTxTransport changeFormatQRCodeTransport(String str) {
        try {
            String[] strArray = QRCodeUtil.splitString(str);
            QRCodeTxTransport qrCodeTransport = new QRCodeTxTransport();
            log.debug("qrcode", "str," + str);
            log.debug("qrcode", "0," + strArray[0]);

            String address = Base58.hexToBase58WithAddress(strArray[0]);
            log.debug("qrcode", "address," + address);
            if (!Utils.validBicoinAddress(address)) {
                return null;
            }
            qrCodeTransport.setMyAddress(address);
            String changeAddress = Base58.hexToBase58WithAddress(strArray[1]);
            if (!Utils.validBicoinAddress(changeAddress)) {
                return null;
            }
            qrCodeTransport.setChangeAddress(changeAddress);
            qrCodeTransport.setChangeAmt(Long.parseLong(strArray[2], 16));
            qrCodeTransport.setFee(Long.parseLong(
                    strArray[3], 16));
            String toAddress = Base58.hexToBase58WithAddress(strArray[4]);
            if (!Utils.validBicoinAddress(toAddress)) {
                return null;
            }
            qrCodeTransport.setToAddress(toAddress);
            qrCodeTransport.setTo(Long.parseLong(
                    strArray[5], 16));
            List<String> hashList = new ArrayList<String>();
            for (int i = 6; i < strArray.length; i++) {
                String text = strArray[i];
                if (!Utils.isEmpty(text)) {
                    hashList.add(text);
                }
            }
            qrCodeTransport.setHashList(hashList);
            return qrCodeTransport;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    private static QRCodeTxTransport noChangeFormatQRCodeTransport(String str) {
        try {
            String[] strArray = QRCodeUtil.splitString(str);
            if (Utils.validBicoinAddress(strArray[0])) {
                return oldFormatQRCodeTransport(str);
            }
            QRCodeTxTransport qrCodeTransport = new QRCodeTxTransport();
            log.debug("qrcode", "str," + str);
            log.debug("qrcode", "0," + strArray[0]);

            String address = Base58.hexToBase58WithAddress(strArray[0]);
            log.debug("qrcode", "address," + address);
            if (!Utils.validBicoinAddress(address)) {
                return null;
            }
            qrCodeTransport.setMyAddress(address);
            qrCodeTransport.setFee(Long.parseLong(
                    strArray[1], 16));
            qrCodeTransport.setToAddress(Base58.hexToBase58WithAddress(strArray[2]));
            qrCodeTransport.setTo(Long.parseLong(
                    strArray[3], 16));
            List<String> hashList = new ArrayList<String>();
            for (int i = 4; i < strArray.length; i++) {
                String text = strArray[i];
                if (!Utils.isEmpty(text)) {
                    hashList.add(text);
                }
            }
            qrCodeTransport.setHashList(hashList);
            return qrCodeTransport;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    private static QRCodeTxTransport oldFormatQRCodeTransport(String str) {
        try {
            String[] strArray = QRCodeUtil.splitString(str);
            QRCodeTxTransport qrCodeTransport = new QRCodeTxTransport();
            String address = strArray[0];
            if (!Utils.validBicoinAddress(address)) {
                return null;
            }
            qrCodeTransport.setMyAddress(address);
            qrCodeTransport.setFee(Long.parseLong(
                    strArray[1], 16));
            qrCodeTransport.setToAddress(strArray[2]);
            qrCodeTransport.setTo(Long.parseLong(
                    strArray[3], 16));
            List<String> hashList = new ArrayList<String>();
            for (int i = 4; i < strArray.length; i++) {
                String text = strArray[i];
                if (!Utils.isEmpty(text)) {
                    hashList.add(text);
                }
            }
            qrCodeTransport.setHashList(hashList);
            return qrCodeTransport;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    private static QRCodeTxTransport oldFromSendRequestWithUnsignedTransaction(Tx tx, String addressCannotParsed) {
        QRCodeTxTransport qrCodeTransport = new QRCodeTxTransport();
        qrCodeTransport.setMyAddress(tx.getFromAddress());
        String toAddress = tx.getFirstOutAddress();
        if (Utils.isEmpty(toAddress)) {
            toAddress = addressCannotParsed;
        }
        qrCodeTransport.setToAddress(toAddress);
        qrCodeTransport.setTo(tx.amountSentToAddress(toAddress));
        qrCodeTransport.setFee(tx.getFee());
        List<String> hashList = new ArrayList<String>();
        for (byte[] h : tx.getUnsignedInHashes()) {
            hashList.add(Utils.bytesToHexString(h));
        }
        qrCodeTransport.setHashList(hashList);
        return qrCodeTransport;
    }

    public static String oldGetPreSignString(Tx tx, String addressCannotParsed) {
        QRCodeTxTransport qrCodeTransport = oldFromSendRequestWithUnsignedTransaction(tx, addressCannotParsed);
        String preSignString = qrCodeTransport.getMyAddress()
                + QRCodeUtil.OLD_QR_CODE_SPLIT
                + Long.toHexString(qrCodeTransport.getFee())
                .toLowerCase(Locale.US)
                + QRCodeUtil.OLD_QR_CODE_SPLIT
                + qrCodeTransport.getToAddress()
                + QRCodeUtil.OLD_QR_CODE_SPLIT
                + Long.toHexString(qrCodeTransport.getTo())
                .toLowerCase(Locale.US) + QRCodeUtil.OLD_QR_CODE_SPLIT;
        for (int i = 0; i < qrCodeTransport.getHashList().size(); i++) {
            String hash = qrCodeTransport.getHashList().get(i);
            if (i < qrCodeTransport.getHashList().size() - 1) {
                preSignString = preSignString + hash + QRCodeUtil.OLD_QR_CODE_SPLIT;
            } else {
                preSignString = preSignString + hash;
            }
        }

        return preSignString;
    }

    public static String oldEncodeQrCodeString(String text) {
        Pattern pattern = Pattern.compile("[A-Z]");
        Matcher matcher = pattern.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String letter = matcher.group(0);
            matcher.appendReplacement(sb, QR_CODE_LETTER + letter);
        }
        matcher.appendTail(sb);

        return sb.toString().toUpperCase(Locale.US);
    }

}
