/*
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to
 * the Free Software Foundation, Inc., 59 Temple Place,
 * Suite 330, Boston, MA 02111-1307 USA
 * 
 * Copyright 2008 Alejandro Pedraza. All rights reserved.
 * 
 * $Date: 2008-05-10 00:01:05 -0500 (Sat, 10 May 2008) $
 * $Revision: 8 $
 * $LastChangedBy: alejandro.pedraza $
 */

package jkeeper;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

/**
 *
 * @author  Alejandro Pedraza <alejandro.pedraza at gmail>
 */
public class JKeeperEncryption {

    static Cipher getCipher(int mode, char[] password) throws Exception {
        byte[] salt = {
                (byte)0xc7, (byte)0x73, (byte)0x21, (byte)0x8c,
                (byte)0x7e, (byte)0xc8, (byte)0xee, (byte)0x99
            };
        int count = 20;
        PBEParameterSpec pbeParamSpec = new PBEParameterSpec(salt, count);
        PBEKeySpec pbeKeySpec = new PBEKeySpec(password);
        SecretKeyFactory keyFac = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
        SecretKey pbeKey = keyFac.generateSecret(pbeKeySpec);
        Cipher pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
        
        switch (mode) {
            case Cipher.ENCRYPT_MODE:
                pbeCipher.init(Cipher.ENCRYPT_MODE, pbeKey, pbeParamSpec);
                break;
            case Cipher.DECRYPT_MODE:
                pbeCipher.init(Cipher.DECRYPT_MODE, pbeKey, pbeParamSpec);
                break;
            default:
                assert(false);
        }
        
        return pbeCipher;
    }
}
