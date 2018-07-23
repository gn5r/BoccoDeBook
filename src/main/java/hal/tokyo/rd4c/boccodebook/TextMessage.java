/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hal.tokyo.rd4c.boccodebook;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 *
 * @author tame
 */
public class TextMessage {

    private static final String textPath = "Text/Message.txt";
    private int cnt;
    private BufferedReader br;

    /* 詳細はText/README.txt参照 */
    public static final int SESSION_OK = 0;
    public static final int FIRST_SET = 1;
    public static final int NOT_FIRST = 2;
    public static final int LAST_SET = 3;
    public static final int NOT_END = 4;
    public static final int CARD_STEP = 5;
    public static final int NOT_CARD = 6;
    public static final int READ_OK = 7;
    public static final int NOT_REC = 8;

    /* return：テキストファイルから読み出したメッセージ文字列 */
    public String readText(int MessageNum) {
        String str = new String();
        cnt = 0;

        try {
            File file = new File(textPath);
           br = new BufferedReader(new FileReader(file));
            str = br.readLine();
            /* 文字列全てから必要なものだけ抽出する */
            while (str != null) {

                if (cnt == MessageNum) {
                    System.out.print(str);
                }
                str = br.readLine();
                cnt++;
            }
            br.close();
        } catch (FileNotFoundException e) {
            /* ファイルが見つからなかった */
            System.out.println(e);
        } catch (IOException e) {
            /* 入出力の例外、割り込み発生によるエラー */
            System.out.println(e);
        } 
        return str;
    }
}
