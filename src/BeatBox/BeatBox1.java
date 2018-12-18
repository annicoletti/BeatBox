/*
 * Copyright (C) 2018 Administrador
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package BeatBox;

/**
 *
 * @author Administrador
 */
import com.sun.org.apache.xml.internal.security.encryption.Serializer;
import javax.swing.*;
import java.awt.*;
import javax.sound.midi.*;
import java.util.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import java.io.*;
import javafx.stage.FileChooser;
import jdk.nashorn.internal.runtime.JSONFunctions;


public class BeatBox1 implements Serializable {
    
    JPanel mainPanel;    
    
    // Armazenaremos as caixas de seleção em uma ArrayList
    ArrayList<JCheckBox> checkBoxList;
    Sequencer sequencer;
    Sequence sequence;
    Track track;
    JFrame theFrame;
    
    //Estes são os nomes dos instrumentos, com uma array de string, 
    //para a construção dos rótulos da GUI (em cada linha)
    String[] instrumentName = {"Bass Drum","Closed Hi-Hat","Open Hi-Hat","Acoustic Snare",
        "Crash Cymbal","Hand CLap","Hight Tom","Hi Bongo","Maracas","Whistle","Low Conga",
        "Cowbell","Vibraslap","Low-mid Tom","High Agogo","Open Hi Conga"};
    
    //Esses números representam as teclas reais da bateria. O canal da bateria é 
    //como o piano, exceto pelo fato de cada tecla do piano ser um elemento de 
    //bateria diferente. Portanto o número 35 é a tecla do bumbo(bass drum), 42
    //é o Hi-Chapéu Closed (Closed Hi-Hat), etc.
    int[] instruments = {32,42,46,38,49,39,50,60,70,72,64,56,58,47,67,63};
    
    public static void main(String[] args) {
        //new BeatBox1().buildGUI();
        BeatBox1 bb = new BeatBox1();
        bb.buildGUI();
    }
    
    public String ver(int i){
        if(i>0){
            return "ok";
        }
        else{
            return "não";
        }
    }
    
    public void buildGUI(){
        theFrame = new JFrame("Cyber BeatBox");
        theFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        BorderLayout layout = new BorderLayout();
        JPanel background = new JPanel(layout);
        
        //Uma borda vazia nos fornecerá uma margem entre as bordas do painel e 
        //onde os componentes estão posicionados. Puramente estética.
        background.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        
        checkBoxList = new ArrayList<JCheckBox>();
        Box buttonBox = new Box(BoxLayout.Y_AXIS);
        
        JButton start = new JButton("Start");
        start.addActionListener(new MyStartListener());
        buttonBox.add(start);
        
        JButton stop = new JButton("Stop");
        stop.addActionListener(new MyStopListener());
        buttonBox.add(stop);
        
        JButton upTempo = new JButton("Tempo Up");
        upTempo.addActionListener(new MyUpTempoListener());
        buttonBox.add(upTempo);
        
        JButton downTempo = new JButton("Tempo Down");
        downTempo.addActionListener(new MyDownTempoListener());
        buttonBox.add(downTempo);
        
        JButton serializeIt = new JButton("Serialize It");
        serializeIt.addActionListener(new MySendListener());
        buttonBox.add(serializeIt);
        
        JButton restore = new JButton("Restore");
        restore.addActionListener(new MyReadListener());
        buttonBox.add(restore);
        
        
        Box nameBox = new Box(BoxLayout.Y_AXIS);
        for(int i=0; i<16; i++){
            nameBox.add(new Label(instrumentName[i]));
        } 
        
        background.add(BorderLayout.EAST, buttonBox);
        background.add(BorderLayout.WEST, nameBox);
        
        theFrame.getContentPane().add(background);
        
        GridLayout grid = new GridLayout(16,16);
        grid.setVgap(1);
        grid.setHgap(2);
        mainPanel = new JPanel(grid);
        background.add(BorderLayout.CENTER, mainPanel);
        
        //Cria as caixas de seleção, configura as com false(apenas para que não
        //estejam marcadas) e adiciona o ArrayList ao painel d GUI
        for(int i=0; i<256; i++){
            JCheckBox c = new JCheckBox();
            c.setSelected(false);
            checkBoxList.add(c);
            mainPanel.add(c);
        } // Fim loop
        
        setUpMidi();
        
        theFrame.setBounds(50,50,300,300);
        theFrame.pack();
        theFrame.setVisible(true);
    } //Feche o método   
    
    
    //O Costumeiro trecho da MIDI para captura do sequenciador, da sequencia e 
    //da faixa. Novamente nada de especial
    public void setUpMidi(){
        try{
            sequencer = MidiSystem.getSequencer();
            sequencer.open();
            sequence = new Sequence(Sequence.PPQ, 4);
            track = sequence.createTrack();
            sequencer.setTempoInBPM(120);
        } 
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Aqui é onde tudo acontece! É o local onde convertemos os estado da caixa 
     * de seleção de eventos MIDI e os adicionaremos a faixa.
     */
    public void buildTrackAndStart(){
        
        //Criaremos uma matriz de 16 elementos para armazernar os valores de um
        //instrumento, com todas as 16 batidas. Se o instrumento tiver que ser 
        //reproduzido nessa batida, o valor desse elemento será a tecla. Se esse
        //instrumento NÃO tiver que ser reproduzido nessa batida, insira um zero. 
        int[] trackList = null;
        
        //Elimina a faixa antiga e cria uma nova
        sequence.deleteTrack(track);
        track = sequence.createTrack();
        
        //Fará isso para cada uma das 16 LINHAS (isto é, Bass, Congo, etc.).
        for (int i=0; i<16; i++){
            trackList = new int[16];
            
            //Configura a tecla que representará qual é esse instrumento(bumbo, 
            //hi-chapeu, etc. A Matriz de instrumento contém os numeros MIDI  
            //reaia de cada instrumento).
            int key = instruments[i];
            
            //Fará isso para cada uma das BATIDAS dessa linha.
            for(int j=0; j<16; j++){
                
                //A caixa de seleção dessa batida está ativada? Se estiver, insira
                //o valor da tecla nessa posição da matriz (a posição que representa 
                //essa batida). Caso contrário, o instrumento NÃO deve reproduzir
                //essa batida, portanto, configure com zero.
                JCheckBox jc = (JCheckBox) checkBoxList.get((j + (16*i)));
                if(jc.isSelected()){
                    trackList[j] = key;
                }
                else{
                    trackList[j] = 0;
                }
            }
            
            //Para esse instrumento, e para todas as 16 batidas, cria eventos e 
            //adiciona à faixa.
            makeTracks(trackList);
            track.add(makeEvent(176,1,127,0,16));
        }
        
        //Queremos nos certificar sempre que HÁ um evento na batida 16 (ela vai 
        //de 0 a 15). Caso contrário, a BeatBox não pode percorrer todas as 16 
        //batidas antes de começar novamente
        track.add(makeEvent(192,9,1,0,15));
        try{
            sequencer.setSequence(sequence);
            
            //Permite que você especifique a quantidade de iterações do loop ou,
            //nesse caso, um loop contínuo.
            sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY);
            
            //AGORA REPRODUZA!
            sequencer.start();
            sequencer.setTempoInBPM(120);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    } // fecha método buildTrackAndStart
    

    public void SaveFile(File f){            
        try {
            ObjectOutputStream ops = new ObjectOutputStream(new FileOutputStream(f));
            ops.writeObject(f);
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
    }
    
    public void OpenFile(File f){
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f));
            BeatBox1 restore = (BeatBox1) ois.readObject();
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
    }
    
    //Classe interna são ountes dos botões
    /**
     * Escutador para salvar 
     */
        
    public class MyStartListener implements ActionListener{
        public void actionPerformed(ActionEvent e){
            buildTrackAndStart();
        }
    }
    
    public class MyStopListener implements ActionListener{
        public void actionPerformed(ActionEvent e){
            sequencer.stop();
        }
    }
    
    public class MyUpTempoListener implements ActionListener{
        public void actionPerformed(ActionEvent e){
            
            //TempoFactor dimensionará o ritimo da sequencia pelo fator fornecido
            //O padrão é 1, 0, portanto, estamos ajustando para cerca de 3% por clique
            float tempoFactor = sequencer.getTempoFactor();
            sequencer.setTempoFactor((float) (tempoFactor * 1.03));
        }
    }
    
    public class MyDownTempoListener implements ActionListener{
        public void actionPerformed(ActionEvent e){
            
            //TempoFactor dimensionará o ritimo da sequencia pelo fator fornecido
            //O padrão é 1, 0, portanto, estamos ajustando para cerca de 3% por clique
            float tempoFactor = sequencer.getTempoFactor();
            sequencer.setTempoFactor((float) (tempoFactor * 0.97));
        }
    }
    
    public class MySendListener implements ActionListener{
        public void actionPerformed (ActionEvent ev){
            try{
                JFileChooser fileChoser = new JFileChooser();
                fileChoser.showSaveDialog(theFrame);
                SaveFile(fileChoser.getSelectedFile());                
            }
            catch(Exception ex){
                ex.printStackTrace();
            }
            
        } //fecha o método 
    } //fecha a classe interna
     
    public class MyReadListener implements ActionListener{
        public void actionPerformed (ActionEvent ev){
            try{
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.showOpenDialog(mainPanel);
                OpenFile(fileChooser.getSelectedFile());                
            }
            catch(Exception ex){
                ex.printStackTrace();
            }
           
            /**
             * Agora interromperá a seuqencia que estiver sendo reproduzido e 
             * reconstruirá a sequencia usando o novo estado das caixas de seleção
             * ArrayList.
             */
           // sequencer.stop();
           // buildTrackAndStart();
            
        } //fecha o método
    } //fecha a classe interna
    
    /**
    * Isso criará eventos para um instrumento de cada vez, para todas as 16 batidas.
    * Portanto, pode capturar um int[] para o bumbo e cada indice da matriz conterá
    * a tecla desse instrumento ou um zero. Se tiver um zero, o instrumento não 
    * deve ser reproduzido nessa batida. Caso contrário, um evento será criado e
    * adicionado à faixa.
    */
    public void makeTracks (int[] list){
        for(int i=0; i<16; i++){
            int key = list[i];
            
            if(key !=0){
                
                //Cria o evento NOTE ON e NOTE OFF e os adiciona à faixa.
                track.add(makeEvent(144,9,key,100,i));
                track.add(makeEvent(128,9,key,100,i+1));
            }
        }
    }
    
    public MidiEvent makeEvent(int comd, int chan, int one, int two,int tick){
        MidiEvent event = null;
        try{
            ShortMessage a = new ShortMessage();
            a.setMessage(comd, chan, one, two);
            event = new MidiEvent(a, tick);
        }   
        catch(Exception e){
            e.printStackTrace();
        }
        return event;
    }
}
