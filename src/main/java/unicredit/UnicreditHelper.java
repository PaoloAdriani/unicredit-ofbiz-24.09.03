package unicredit;

import java.math.BigDecimal;

import it.netsw.apps.igfs.cg.coms.api.BaseIgfsCg.CurrencyCode;
import it.netsw.apps.igfs.cg.coms.api.BaseIgfsCg.LangID;

public class UnicreditHelper {

    public static final String MODULE = UnicreditHelper.class.getName();

    public static String getSubstringFromSplit(String stringToSplit, int index) {
        String[] strSplit = stringToSplit.split("_");
        return strSplit[index];
    }

    public static CurrencyCode getCurrency(String orhCurrency) {
        CurrencyCode currency = null;
        switch (orhCurrency) {
            case "EUR":
                currency = CurrencyCode.EUR;
                break;
            case "USD":
                currency = CurrencyCode.USD;
                break;
            case "CHF":
                currency = CurrencyCode.CHF;
                break;
            case "GBP":
                currency = CurrencyCode.GBP;
                break;
            default:
                break;
        }
        return currency;
    }

    public static LangID getLanguage(String locale) {
        LangID lang = null;
        switch (locale) {
            case "IT":
                lang = LangID.IT;
                break;
            case "EN":
                lang = LangID.EN;
                break;
            case "US":
                lang = LangID.EN;
                break;
            default:
                break;
        }
        return lang;
    }


    public static Long parseBigDecimalToLong(BigDecimal orderAmount) {
        BigDecimal virtual_comma_amount = orderAmount.multiply(new BigDecimal("100"));
        Long virtual_comma_amount_long = Long.valueOf(virtual_comma_amount.longValue());
        return virtual_comma_amount_long;
    }

    public static String findDescriptionByRcOutcome(String rc) {
        String description = "_NA_";
        switch (rc) {
            case "IGFS_000":
                description = "TRANSAZIONE OK";
                break;
            case "IGFS_890":
                description = "TRANSAZIONE IN CORSO (stato operazione indefinito)";
                break;
            case "IGFS_814":
                description = "TRANSAZIONE IN CORSO";
                break;
            case "IGFS_00157":
                description = "STRUMENTO PAGAMENTO NON VALIDO";
                break;
            case "IGFS_00158":
                description = "NUMERO CARTA NON NUMERICO";
                break;
            case "IGFS_00159":
                description = "NUMERO CARTA NON PRESENTE";
                break;
            case "IGFS_008":
                description = "AUTORIZZAZIONE NEGATA";
                break;
            case "IGFS_01000":
                description = "TRANSAZIONE NEGATA DAL SISTEMA ANTIFRODE";
                break;
            case "IGFS_020":
                description = "CARTA INVALIDA";
                break;
            case "IGFS_086":
                description = "MALFUNZIONAMENTO SISTEMA";
                break;
            case "IGFS_122":
                description = "ERRORE SICUREZZA";
                break;
            case "IGFS_20090":
                description = "TRANSAZIONE CANCELLATA DALL'UTENTE";
                break;
            case "IGFS_90000":
                description = "DATABASE ERROR";
                break;
            case "IGFS_902":
                description = "TRANSAZIONE NON VALIDA";
                break;
            case "IGFS_909":
                description = "ERRORE DI SISTEMA";
                break;
            default:
                break;
        }
        return description;
    }

} //end class
