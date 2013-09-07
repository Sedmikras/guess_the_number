package icp.online.app;

import java.util.LinkedList;

import icp.online.tcpip.objects.RDA_Marker;
import icp.online.tcpip.objects.RDA_MessageData;

/**
 * Název úlohy: Jednoduché BCI
 * Třída: Buffer
 * @author Bohumil Podlesák
 * První verze vytvořena: 28.3.2010
 * @version 2.0
 * 
 * Třída, do které se mohou průběžně ukládat datové objekty RDA MessageData.
 * Tyto objekty jsou následně zpracovány a mohou být vráceny jako pole typu float.
 */
public class Buffer {
	/**
	 * Rezerva pro zaplněnost pole bufferu.
	 * Určuje, kolik položek zůstane maximálně volných, aby metoda jePlny() vratila true.
	 */
	private static final int REZERVA = 20;
	/**
	 * Určuje počet hodnot příslušející jedné elektrodě v jednom datovém bloku.
	 */
	private static final int POCETHODNOTELEKTRODY = 20;	
	/**
	 * Index elektrody FZ. Tato elektroda je v poli zapsána jako šestnáctá v pořadí.
	 */
	private static final int INDEXFZ = 16;
	/**
	 * Index elektrody PZ. Tato elektroda je v poli zapsána jako sedmnáctá v pořadí.
	 */
	private static final int INDEXPZ = 17;
	/**
	 * Index elektrody FZ. Tato elektroda je v poli zapsána jako osmnáctá v pořadí.
	 */
	private static final int INDEXCZ = 18;
		
	private float[] data;
	private int konec;
	private int delka;
	private int predMarkerem;
	private int zaMarkerem;	
	private LinkedList<Integer> frontaIndexu;
	private LinkedList<Integer> frontaTypuStimulu;

	
	/**
	 * Konstruktor
	 * Z prvu je nutné začínat plnit buffer až od indexu predMarkerem, protože jinak by
	 * v případě brzkého příchodu markeru bylo nutno vybírat hodnoty mimo rozsah pole.
	 * Délka pole hodnot v Bufferu by měla být mnohem větší než predMarkerem + zaMarkerem
	 * @param delka - počáteční délka pole, do kterého Buffer ukládá hodnoty
	 * @param predMarkerem - počet položek pole, které se budou vybírat před markerem
	 * @param zaMarkerem - počet položek pole, které se budou vybírat za markerem
	 */
	public Buffer(int delka, int predMarkerem, int zaMarkerem){
		this.delka = delka;
		this.data = new float[this.delka];
		for(int i = 0; i < this.delka; i++){
			this.data[i] = Float.MAX_VALUE;
		}
		this.konec = predMarkerem;
		this.predMarkerem = predMarkerem;
		this.zaMarkerem = zaMarkerem;
		this.frontaIndexu = new LinkedList<Integer>();
		this.frontaTypuStimulu = new LinkedList<Integer>();
	}
	
	/**
	 * Výběr hodnot pouze ze tří elektrod (FZ, PZ, CZ).
	 * Data ze těhto tří elektrod se zprůměrují váženým aritmetickým průměrem.
	 * @param pole - pole hodnot, ve kterém jsou záznamy všech elektrod
	 * @param pomerFZ - poměr pro vážený průměr z elektrody FZ
	 * @param pomerPZ - poměr pro vážený průměr z elektrody PZ
	 * @param pomerCZ - poměr pro vážený průměr z elektrody CZ
	 * @return zprůměrované pole vypočtené z elektrod FZ, PZ a CZ
	 */	
	private float[] vyberFZPZCZ(float[] pole, int pomerFZ, int pomerPZ, int pomerCZ){
		float[] vybraneHodnoty = new float[POCETHODNOTELEKTRODY];
		for(int i = 0; i < POCETHODNOTELEKTRODY; i++){
			vybraneHodnoty[i] = (pomerFZ * pole[INDEXFZ + 19*i] + pomerPZ * pole[INDEXPZ + 19*i]
			               + pomerCZ * pole[INDEXCZ + 19*i]) / (pomerFZ + pomerPZ + pomerCZ);
		}
		return vybraneHodnoty;
	}
	
	/**
	 * Zápis dat do bufferu (do pole data) a přidání markeru do fronty.
	 * Pokud by byl buffer příliš zaplněn a nevešly by se do něj informace o dalším prvku,
	 * pak se jeho velikost zdvojnásobí. V ideálním případě se ale bude používat tak,
	 * aby jeho zvětšování nemuselo nastávat.
	 * @param datObjekt - objekt, obsahující pole dat pro zápis do bufferu
	 */
	public void zapis(RDA_MessageData datObjekt){
		float[] hodnoty = vyberFZPZCZ(datObjekt.getfData(), 1, 1, 1);
		/* pokud zbyva malo mista v bufferu */
		if(this.delka - this.konec <= hodnoty.length){
			this.data = zvetsi();//pak zvetsi pole hodnot
		}
		/* zapis hodnot do pole bufferu */
		for(int i = 0; i < hodnoty.length; i++){
			this.data[this.konec + i] = hodnoty[i];
		}		
		RDA_Marker[] markery = datObjekt.getMarkers();
		if(markery != null){
			for(int i = 0; i < markery.length; i++){
				/* promenna index znaci index prave vybraneho markeru;
				   je to aktualni pozice v poli (neaktualizovany this.konec po nahrani novych dat)
				   + relativni pozice markeru uvnitr datoveho objektu */
				int index = this.konec + (int)markery[i].getnPosition();
				this.frontaIndexu.addLast(index);
				/* nutno ulozit zaroven index stimulu do fronty */
				this.frontaTypuStimulu.addLast(Integer.parseInt(markery[i].getsTypeDesc().substring(11,13).trim()) - 1);
			}
		}
		
		this.konec += hodnoty.length;
	}
	
	/**
	 * Dvojnásobné zvětšení stávajícího pole dat.
	 * Zároveň dojde k nastavení všech potřebných atributů třídy
	 * (délka, index začátku a konce)
	 * @return - nové dvojnásobně zvětšené pole
	 */
	private float[] zvetsi(){
		System.out.println("*** VOLANA METODA ZVETSI Z INTANCE BUFFERU ***");
		int novaDelka = 2 * this.delka;
		float[] novaData = new float[novaDelka];
		for(int i = 0; i < this.konec; i++){
			novaData[i] = this.data[i];
		}
		for(int i = this.konec; i < novaDelka; i++){
			novaData[i] = Float.MAX_VALUE;
		}
		this.delka = novaDelka;
		return novaData;
	}
	
	/**
	 * Výběr z Bufferu.
	 * Pokud není v bufferu žádný marker, pak v něm jistě nejsou už žádná data, která by
	 * měla nějakou hodnotu (ve smyslu vybírání pole dat pro Epochu) a metoda vrati null.
	 * Pokud neni v bufferu zapsanych dost hodnot za poslednim markerem, metoda take vrati null.
	 * Tato metoda bude vybírat hodnoty z bufferu podle markerů postupně (FIFO).
	 * @return - pole floatů o délce (this.pred + this.po), obsahující hodnoty z bufferu
	 * kolem posledního markeru plus číslo Stimulu (0 - 9)
	 */
	public HodnotyVlny vyber(){
		if(this.frontaIndexu.isEmpty()){
			return null;
		}
		/* pokud neni k vybrani epochy kolem markeru jeste nacteno dostatek hodnot
		   (moznost navraceni Float.MAX_VALUE nebo prekroceni delky pole) */
		if(this.frontaIndexu.peek() + this.zaMarkerem > this.konec){
			/* pak se take vrati null */
			return null;
		}
		float[] vybraneHodnoty = new float[this.predMarkerem + this.zaMarkerem];
		/* index markeru minus pocet hodnot pred markerem je indexem prvni polozky, kterou vybereme */
		/* nutno kvuli zaznamenani, na ktery stimul byla tato reakce zaznamenana */
		int typVlny = this.frontaTypuStimulu.removeFirst();
		int index = this.frontaIndexu.removeFirst() - this.predMarkerem;		
		for(int i = 0; i < (this.predMarkerem + this.zaMarkerem); i++){
			vybraneHodnoty[i] = this.data[index + i];
		}
		return new HodnotyVlny(vybraneHodnoty,typVlny);
	}
	
	/**
	 * Metoda pro kontrolu zaplněnosti bufferu.
	 * Slouži k tomu, aby třída, která s ním bude pracovat vědela, kdy má začít vybírat prvky
	 * @return - true, když je buffer plný (zbývá v něm méně místa než REZERVA)
	 */
	public boolean jePlny(){
		return (this.delka - this.konec <= REZERVA);
	}
	
	/**
	 * Uvolní buffer. Doporučeno provádět pokaždé, když se vyberou všechny hodnoty, ktere
	 * jdou pomoci metody vyber().
	 * Tato metoda nastaví položky pole float[] data na Float.MAX_VALUE.
	 * Nebudou však vymazány všechy položky, ponechá se několik položek před koncem (this.konec)
	 * toto je z důvodu, aby v případě brzkého příchodu markeru mohl tento odkazovat na
	 * staré položky. Marker označuje pozici, před kterou se berou data v metodě vyber(). 
	 * Je nutno pro nejhorší případ (marker přijde hned v prvním objektu) ponechat nejméně
	 * this.predMarkerem starých hodnot.
	 * Pokud v bufferu zustal nejaky marker, tak se musi ponechat vice polozek.
	 * V takovem pripade se v promazanem bufferu musi na pocatku objevit this.predMarkerem
	 * hodnot pred indexem markeru ze stareho bufferu a zaroven i vsechna data po nem.
	 * Toto se provede z duvodu, aby dalsi prichozi data mohla navazat na minula data a
	 * neztratil se tak zbytecne jeden blok.
	 * Z tohoto duvodu vyplyva, ze tato metoda vymaze pouze minimum polozek, pokud se vola
	 * velmi casto, respektive kdyz je fronta markeru skoro plna (probihalo malo vyberu).
	 */
	public void vymaz(){
		/* indexPredMarkerem znaci pozici v poli, kde byl nalezen marker, ktery je na rade ve fronte,
		   minus pocet hodnot pred timto markerem, ktere se musi zachovat;
		   jinymi slovy, je to index prvniho nevraceneho datoveho bloku (vetsinou
		   nekompletniho), ktery se pouzije jako pole hodnot pro konstrukci epochy */		
		int indexPredMarkerem;
		if(this.frontaIndexu.peekFirst() == null){
			/* pokud zadny marker ve fronte neni, bude se brat nejhorsi pripad a to ten, ze
			   marker prijde prave nasledujici polozku po posledni hodnote (this.data[this.konec])*/
			indexPredMarkerem = this.konec - this.predMarkerem;
		}else{
			/* jinak standardne this.predMarkerem hodnot pred markerem */
			indexPredMarkerem = this.frontaIndexu.peek() - this.predMarkerem;
		}
		
		/* this.konec minus index je delka bloku (nebo vice bloku dohromady) */
		for(int i = 0; i < this.konec - indexPredMarkerem; i++){
			/* tento se prekopiruje na pocatek pole, pouzitim in-place algoritmu */
			this.data[i] = this.data[indexPredMarkerem + i];
		}
		/* zbytek pole se vymaze - nastavi se hodnota Float.MAX_VALUE */
		for(int i = this.konec - indexPredMarkerem; i < this.delka; i++){
			this.data[i] = Float.MAX_VALUE;
		}
		
		/* pokud zustaly indexy markeru ve fronte indexu, musi se prepsat na nove hodnoty */
		LinkedList<Integer> novaFrontaIndexu = new LinkedList<Integer>();
		while(!this.frontaIndexu.isEmpty()){
			/* všechny indexy markerů z fronty se musí přepsat -> odečíst od nich
			   indexPredMarkerem; tím se všechny posunou na začátek  */
			int indexMarkeru = this.frontaIndexu.removeFirst() - indexPredMarkerem;			
			novaFrontaIndexu.add(indexMarkeru);
		}
		this.frontaIndexu = novaFrontaIndexu;
		
		/* novy konec bude nyni o index prvniho nezpracovaneho datoveho bloku mensi
		   lze si to predstavit, jako ze zadne polozky pole pred timto blokem jiz
		   neexistuji, a tak se tento blok zacne cislovat od nuly */
		this.konec = this.konec - indexPredMarkerem;
	}
	
	public int kolikIndexu(){
		return this.frontaIndexu.size();
	}
	
	public int kolikStimulu(){
		return this.frontaTypuStimulu.size();
	}
}
