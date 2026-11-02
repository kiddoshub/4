package com.gaspoweredcake.client33.utils.misc.input;

/** Semantic migration of the older raw GLFW keybind format into SDL physical codes. */
public final class LegacyInputCodes {
    private LegacyInputCodes() { }
    public static int keyboard(int key) {
        if(key>=65&&key<=90)return key-65+4;
        if(key>=49&&key<=57)return key-49+30;
        if(key>=290&&key<=301)return key-290+58;
        if(key>=302&&key<=313)return key-302+104;
        if(key>=321&&key<=329)return key-321+89;
        return switch(key) {
            case 32->44;case 39->52;case 44->54;case 45->45;case 46->55;case 47->56;case 48->39;
            case 59->51;case 61->46;case 91->47;case 92->49;case 93->48;case 96->53;
            case 161->135;case 162->100;
            case 256->41;case 257->40;case 258->43;case 259->42;case 260->73;case 261->76;
            case 262->79;case 263->80;case 264->81;case 265->82;case 266->75;case 267->78;case 268->74;case 269->77;
            case 280->57;case 281->71;case 282->83;case 283->70;case 284->72;
            case 320->98;case 330->99;case 331->84;case 332->85;case 333->86;case 334->87;case 335->88;case 336->103;
            case 340->225;case 341->224;case 342->226;case 343->227;case 344->229;case 345->228;case 346->230;case 347->231;case 348->101;
            default->0;
        };
    }
    public static int mouse(int button) {return switch(button) {case 0->1;case 1->3;case 2->2;default->button>=3&&button<=7?button+1:0;};}
    public static int modifiers(int mask) {
        int result=0;
        if((mask&1)!=0)result|=3;
        if((mask&2)!=0)result|=192;
        if((mask&4)!=0)result|=768;
        if((mask&8)!=0)result|=3072;
        if((mask&16)!=0)result|=8192;
        if((mask&32)!=0)result|=4096;
        return result;
    }
}
