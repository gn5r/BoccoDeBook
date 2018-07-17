/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hal.tokyo.rd4c.boccodebook;

import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import hal.tokyo.rd4c.speech2text.Speaker;
import java.util.Random;

/**
 *
 * @author gn5r
 */
public class SEButtonListener implements GpioPinListenerDigital {

    private GpioPinDigitalOutput buttonLED;
    private String fileName;
    private String eventComedy = "event/comedy/";
    private String eventFantasy = "event/fantasy/";
    private String eventHorror = "event/horror/";

    public SEButtonListener(GpioPinDigitalOutput buttonLED, int BGMNum) {
        this.buttonLED = buttonLED;
        this.fileName = selectRundomSE(BGMNum);

        /*    該当ボタンのLEDを点灯    */
        this.buttonLED.high();
    }

    @Override
    public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent gpdsce) {
        
        try {

            if (gpdsce.getState() == PinState.LOW) {
                System.out.println("押されたピン:" + gpdsce.getPin());
                Speaker speaker = new Speaker();
                /*    LED消灯    */
                this.buttonLED.low();
                /*    ジャンルの音声をランダムに1つ選択    */
                speaker.openFile(this.fileName);
                speaker.playSE();
                speaker.stopSE();

            } else {
                this.buttonLED.high();
            }

        } catch (Exception e) {
        }
    }

    private String selectRundomSE(int BGMNum) {
        String selectFile = null;

        Random random = new Random();
        int SENum = random.nextInt(3) + 1;

        /*    BGM番号
                 0~2 : start
                 3~11 : event
                 12~14 : end
         */
        switch (BGMNum) {
            /*    startカード    */
            case 0:
            case 1:
            case 2:
                selectFile = "start/" + BGMNum + "/" + SENum + ".wav";
                break;

            /*    event  commedy    */
            case 3:
            case 4:
            case 5:
                selectFile = eventComedy + BGMNum + "/" + SENum + ".wav";
                break;

            /*    event fantasy*/
            case 6:
            case 7:
            case 8:
                selectFile = eventFantasy + BGMNum + "/" + SENum + ".wav";
                break;

            /*    event horror*/
            case 9:
            case 10:
            case 11:
                selectFile = eventHorror + BGMNum + "/" + SENum + ".wav";
                break;

            /*    endカード    */
            case 12:
            case 13:
            case 14:
                /*    1:happy 2:normal 3:bad    */
                selectFile = "end/" + BGMNum + "/" + SENum + ".wav";
                break;
        }

        return selectFile;
    }
}
