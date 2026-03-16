package unicredit;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.order.order.OrderReadHelper;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import unicredit.logger.UnicreditLogger;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.Map;

import it.netsw.apps.igfs.cg.coms.api.BaseIgfsCg.CurrencyCode;
import it.netsw.apps.igfs.cg.coms.api.BaseIgfsCg.LangID;
import it.netsw.apps.igfs.cg.coms.api.IgfsException;
import it.netsw.apps.igfs.cg.coms.api.init.IgfsCgInit;
import it.netsw.apps.igfs.cg.coms.api.init.IgfsCgInit.TrType;
import it.netsw.apps.igfs.cg.coms.api.init.IgfsCgVerify;

public class UnicreditServices {

    public static final String MODULE = UnicreditServices.class.getName();
    public static final String SYSTEM_RESOURCE_ID = "unicredit";

    public static String initRequest(HttpServletRequest request, HttpServletResponse response) {
        Locale locale = UtilHttp.getLocale(request);
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");

        //Get system user login
        GenericValue systemUserLogin = UnicreditWorker.getSystemUserLogin(delegator);

        String serverBaseURL = "";
        String redirectNotifyURL = "";
        String redirectErrorURL = "";
        String tId = "";
        String kSig = "";

        /* 1 - Read environment type (Live / Sandbox) */
        String environment = EntityUtilProperties.getPropertyValue(SYSTEM_RESOURCE_ID, "api.environment", delegator);

        String username = EntityUtilProperties.getPropertyValue(SYSTEM_RESOURCE_ID,"unicredit.username", delegator);
        String password = EntityUtilProperties.getPropertyValue(SYSTEM_RESOURCE_ID,"unicredit.password", delegator);

        Debug.logWarning("#### environment [" + environment + "] ###", MODULE);

        if ("test".equals(environment)) {
            serverBaseURL = EntityUtilProperties.getPropertyValue(SYSTEM_RESOURCE_ID, "serverBaseURL.test", delegator);
            redirectNotifyURL = EntityUtilProperties.getPropertyValue(SYSTEM_RESOURCE_ID, "redirectNotifyURL.test",
                    delegator);
            redirectErrorURL = EntityUtilProperties.getPropertyValue(SYSTEM_RESOURCE_ID, "redirectErrorURL.test",
                    delegator);
            tId = EntityUtilProperties.getPropertyValue(SYSTEM_RESOURCE_ID, "tId.test", delegator);
            kSig = EntityUtilProperties.getPropertyValue(SYSTEM_RESOURCE_ID, "kSig.test", delegator);

        } else {

            serverBaseURL = EntityUtilProperties.getPropertyValue(SYSTEM_RESOURCE_ID, "serverBaseURL.prod", delegator);
            redirectNotifyURL = EntityUtilProperties.getPropertyValue(SYSTEM_RESOURCE_ID, "redirectNotifyURL.prod",
                    delegator);
            redirectErrorURL = EntityUtilProperties.getPropertyValue(SYSTEM_RESOURCE_ID, "redirectErrorURL.prod",
                    delegator);
            tId = EntityUtilProperties.getPropertyValue(SYSTEM_RESOURCE_ID, "tId.prod", delegator);
            kSig = EntityUtilProperties.getPropertyValue(SYSTEM_RESOURCE_ID, "kSig.prod", delegator);

        }

        Debug.logWarning("#### serverBaseURL [" + serverBaseURL + "] ###", MODULE);
        Debug.logWarning("#### redirectNotifyURL [" + redirectNotifyURL + "] ###", MODULE);
        Debug.logWarning("#### redirectErrorURL [" + redirectErrorURL + "] ###", MODULE);
        Debug.logWarning("#### tId [" + tId + "] ###", MODULE);
        Debug.logWarning("#### kSig [" + kSig + "] ###", MODULE);

        String orderId = (String) request.getAttribute("orderId");

        if (orderId == null) {
            Debug.logError("### orderId not found in request. Cannot proceed with payamnet processing.", MODULE);
            return "error";
        }

        Debug.logWarning("#### Called initRequest method of UnicreditService for order id [" + orderId + "] ###",
                MODULE);

        UnicreditLogger logger = new UnicreditLogger(delegator.getDelegatorTenantId());

        logger.logInfo("#### Called initRequest method of UnicreditService for order id [" + orderId + "] ###");

        try {

            Debug.logWarning("#### Start retrieve fiels for IgfsCgInit object for order id [" + orderId + "] ###",
                    MODULE);
            logger.logInfo("#### Start retrieve fiels for IgfsCgInit object for order id [" + orderId + "] ###");

            /* Get the orderHeader and OrderReadHelper */
            GenericValue orderHeader = UnicreditWorker.findOrderHeaderFromOrderId(delegator, orderId);

            OrderReadHelper orh = new OrderReadHelper(orderHeader);

            BigDecimal orderAmount = orh.getOrderGrandTotal();

            Long amount = UnicreditHelper.parseBigDecimalToLong(orderAmount);

            UnicreditWorker.updateOrderHeader(delegator, orderId, null, null, null, amount);

            Debug.logWarning("#### orderAmount [" + amount + "] ###",MODULE);

            GenericValue productStore = orh.getProductStore();

            String localeStr = (String) productStore.get("defaultLocaleString");

            // Use the Store Locale
            if (localeStr != null && UtilValidate.isNotEmpty(localeStr)) {
                locale = UtilMisc.parseLocale(localeStr);
            }

            String orderCurrencyUomId = orh.getCurrency();

            CurrencyCode currencyCode = UnicreditHelper.getCurrency(orderCurrencyUomId);

            String locale2Iso = UnicreditHelper.getSubstringFromSplit(localeStr, 1);

            LangID langID = UnicreditHelper.getLanguage(locale2Iso);

            String orderEmail = orh.getOrderEmailString();

            Debug.logWarning("####  serverBaseURL [" + serverBaseURL + "] ###", MODULE);
            Debug.logWarning("####  tId [" + tId + "] ###", MODULE);
            Debug.logWarning("####  kSig [" + kSig + "] ###", MODULE);
            Debug.logWarning("####  orderId [" + orderId + "] ###", MODULE);
            Debug.logWarning("####  orderEmail [" + orderEmail + "] ###", MODULE);
            Debug.logWarning("####  currencyCode [" + currencyCode + "] ###", MODULE);
            Debug.logWarning("####  langID [" + langID + "] ###", MODULE);
            Debug.logWarning("####  Amount [" + amount + "] ###", MODULE);
            Debug.logWarning("####  redirectErrorURL [" + redirectErrorURL + "] ###", MODULE);
            Debug.logWarning("####  redirectNotifyURL [" + redirectNotifyURL + "] ###", MODULE);

            StringBuffer redirectNotifyURLComplete = new StringBuffer();
            redirectNotifyURLComplete.append(redirectNotifyURL).append("?").append("shopID=").append(orderId);

            IgfsCgInit init = new IgfsCgInit();

            init.setServerURL(new URL(serverBaseURL));
            init.setTimeout(15000);
            init.setTid(tId);
            init.setKSig(kSig);
            init.setShopID(orderId);
            init.setShopUserRef(orderEmail);
            init.setTrType(TrType.PURCHASE);
            init.setCurrencyCode(currencyCode);
            init.setLangID(langID);
            init.setAmount(amount);
            init.setErrorURL(new URL(redirectErrorURL));
            init.setNotifyURL(new URL(redirectNotifyURLComplete.toString()));

            // ====================================================================
            // = esecuzione richiesta di inizializzazione
            // ====================================================================

            Debug.logWarning("#### Call IgfsCgInit execute for order id [" + orderId + "] ###",
                    MODULE);
            logger.logInfo("#### Call IgfsCgInit execute for order id [" + orderId + "] ###");


            if (!init.execute())
            {
                // ====================================================================
                // = redirect del client su pagina di errore definita dall’Esercente =
                // ====================================================================

                Debug.logWarning("#### Redirect to error URL for order id [" + orderId + "] ###",
                        MODULE);
                logger.logInfo("#### Redirect to error URL for order id [" + orderId + "] ###");

                String msg = "Unicredit init has a problem with this outcome: "+init.getRc()+" and this description: "+init.getErrorDesc();

                UnicreditWorker.createOrderHeaderNote(dispatcher, orderId, msg, username, password);

                boolean orderCancelled = UnicreditWorker.cancelOrder(dispatcher, systemUserLogin, orderId);

                Debug.logWarning("### orderId [" + orderId + "] ### cancelled with status: "+orderCancelled+" ###",MODULE);
                logger.logInfo("############# orderId [" + orderId + "] ### cancelled with status: "+orderCancelled);

                String errorDescription = init.getErrorDesc().replace(" ", "_");

                Debug.logWarning("### errorDescription [" + errorDescription + "] ###",MODULE);

                response.sendRedirect(redirectErrorURL + "?rc=" + init.getRc() + "&errorDesc=" +errorDescription+ "&shopID=" +orderId);
                return "error";

            }else {

                Debug.logWarning("#### Getting paymentID from IgfsCgInit for order id [" + orderId + "] ###",
                        MODULE);
                logger.logInfo("#### Getting paymentID from IgfsCgInit for order id [" + orderId + "] ###");

                String paymentID = init.getPaymentID();

                // NOTA: Salvo il paymentID relativo alla richiesta (es. sul DB)...
                // ====================================================================
                // = redirect del client verso URL PagOnline BuyNow
                // ====================================================================
                Debug.logWarning("#### Update orderHeader for order id [" + orderId + "] with paymentID [" + paymentID + " ] from IgfsCgInit object ###",
                        MODULE);
                logger.logInfo("#### Update orderHeader for order id [" + orderId + "] with paymentID [" + paymentID + " ] from IgfsCgInit object ###");

                boolean orderHeaderUpdated = UnicreditWorker.updateOrderHeader(delegator, orderId, paymentID, null, null, null);

                Debug.logWarning("#### OrderHeader for order id [" + orderId + "] updated with this status [" + orderHeaderUpdated + " ]. Redirect to notify url... ###",
                        MODULE);
                logger.logInfo("#### OrderHeader for order id [" + orderId + "] updated with this status [" + orderHeaderUpdated + " ]. Redirect to notify url... ###");

                URL redirectURL = init.getRedirectURL();
                response.sendRedirect(redirectURL.toString());
            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            Debug.logError(e.getMessage(), MODULE);
            return "error";
        } catch (IgfsException e) {
            // TODO Auto-generated catch block
            Debug.logError(e.getMessage(), MODULE);
            return "error";
        }

        return "success";

    }

    /*
     * This method is called by Unicredit after the payment process on their platform.
     * With this method we verify the payment and update the order status accordingly.
     */
    public static String notifyRequest(HttpServletRequest request, HttpServletResponse response) {

        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");

        UnicreditLogger logger = new UnicreditLogger(delegator.getDelegatorTenantId());

        Debug.logWarning("### Start of Unicredit Notify Verify Handling ###",MODULE);
        logger.logInfo("### Start of Unicredit Notify Verify Handling ###");

        String environment = EntityUtilProperties.getPropertyValue(SYSTEM_RESOURCE_ID, "api.environment", delegator);

        String username = EntityUtilProperties.getPropertyValue(SYSTEM_RESOURCE_ID,"unicredit.username", delegator);
        String password = EntityUtilProperties.getPropertyValue(SYSTEM_RESOURCE_ID,"unicredit.password", delegator);

        //Get system user login
        GenericValue systemUserLogin = UnicreditWorker.getSystemUserLogin(delegator);

        Debug.logWarning("#### environment [" + environment + "] ###", MODULE);

        String serverBaseURL = "";
        String redirectSuccessURL = "";
        String redirectErrorURL = "";
        String tId = "";
        String kSig = "";

        if ("test".equals(environment)) {
            serverBaseURL = EntityUtilProperties.getPropertyValue(SYSTEM_RESOURCE_ID, "serverBaseURL.test", delegator);
            redirectSuccessURL = EntityUtilProperties.getPropertyValue(SYSTEM_RESOURCE_ID, "redirectSuccessURL.test",
                    delegator);
            redirectErrorURL = EntityUtilProperties.getPropertyValue(SYSTEM_RESOURCE_ID, "redirectErrorURL.test",
                    delegator);
            tId = EntityUtilProperties.getPropertyValue(SYSTEM_RESOURCE_ID, "tId.test", delegator);
            kSig = EntityUtilProperties.getPropertyValue(SYSTEM_RESOURCE_ID, "kSig.test", delegator);

        } else {

            serverBaseURL = EntityUtilProperties.getPropertyValue(SYSTEM_RESOURCE_ID, "serverBaseURL.prod", delegator);
            redirectSuccessURL = EntityUtilProperties.getPropertyValue(SYSTEM_RESOURCE_ID, "redirectSuccessURL.prod",
                    delegator);
            redirectErrorURL = EntityUtilProperties.getPropertyValue(SYSTEM_RESOURCE_ID, "redirectErrorURL.prod",
                    delegator);
            tId = EntityUtilProperties.getPropertyValue(SYSTEM_RESOURCE_ID, "tId.prod", delegator);
            kSig = EntityUtilProperties.getPropertyValue(SYSTEM_RESOURCE_ID, "kSig.prod", delegator);

        }

        Debug.logWarning("#### serverBaseURL [" + serverBaseURL + "] ###", MODULE);
        Debug.logWarning("#### redirectSuccessURL [" + redirectSuccessURL + "] ###", MODULE);
        Debug.logWarning("#### redirectErrorURL [" + redirectErrorURL + "] ###", MODULE);
        Debug.logWarning("#### tId [" + tId + "] ###", MODULE);
        Debug.logWarning("#### kSig [" + kSig + "] ###", MODULE);


        String orderId = request.getParameter("shopID");

        Debug.logWarning("### Getting shopID reference ["+orderId+"] from response ###",MODULE);
        logger.logInfo("### Getting shopID reference ["+orderId+"] from response ###");

        try
        {

            String orderPaymentID = UnicreditWorker.findMpPaymentIdFromOrderId(delegator, orderId);

            GenericValue orderHeader = UnicreditWorker.findOrderHeaderFromOrderId(delegator, orderId);

            OrderReadHelper orh = new OrderReadHelper(orderHeader);

            GenericValue productStore = orh.getProductStore();

            String orderStatus = orderHeader.getString("statusId");

            if(orderPaymentID != null && (orderStatus.equals("ORDER_APPROVED") || orderStatus.equals("ORDER_COMPLETED")))
            {
                Debug.logWarning("### (notifyRequest method) Order ["+orderId+"] is in that status ["+orderStatus+"]. Return Success. Don't process order. ###",MODULE);
                logger.logInfo("### (notifyRequest method) Order ["+orderId+"] is in that status ["+orderStatus+"]. Return Success. Don't process order. ###");

                return "success";
            }

            Debug.logWarning("### Getting paymentID from ["+orderId+"] to orderHeader ###",MODULE);
            logger.logInfo("### Getting paymentID from ["+orderId+"] to orderHeader ###");

            Debug.logWarning("### Start create IgfsCgVerify object ###",MODULE);
            logger.logInfo("### Start create IgfsCgVerify object ###");

            IgfsCgVerify verify = new IgfsCgVerify();

            verify.setServerURL(new URL(serverBaseURL));
            verify.setTimeout(15000);
            verify.setTid(tId);
            verify.setKSig(kSig);
            verify.setShopID(orderId);
            verify.setPaymentID(orderPaymentID);

            if (!verify.execute())
            {
                // ====================================================================
                // = redirect del client su pagina di errore definita dall’Esercente =
                // ====================================================================
                Debug.logWarning("### Verify not executed for these reasons: rc ["+verify.getRc()+"] error description ["+verify.getErrorDesc()+"] ###",MODULE);
                String msg = "Verify not executed for these reasons: rc ["+verify.getRc()+"], error description ["+verify.getErrorDesc() + "]";
                logger.logInfo(msg);

                UnicreditWorker.createOrderHeaderNote(dispatcher, orderId, msg, username, password);

                boolean orderCancelled = UnicreditWorker.cancelOrder(dispatcher, systemUserLogin, orderId);

                Debug.logWarning("### orderId [" + orderId + "] ### cancelled with status: "+orderCancelled+" ###",MODULE);
                logger.logInfo("############# orderId [" + orderId + "] ### cancelled with status: "+orderCancelled);

                Debug.logWarning("### redirect to error URL... ###",MODULE);
                logger.logInfo("############# redirect to error URL...###");

                String errorDescription = verify.getErrorDesc().replace(" ", "_");

                Debug.logWarning("### errorDescription [" + errorDescription + "] ###",MODULE);

                response.sendRedirect(
                        redirectErrorURL + "?rc=" + verify.getRc() + "&errorDesc=" +errorDescription+ "&shopID=" +orderId);

                return "error";

            }else {

                StringBuffer resultUrl = new StringBuffer();
                resultUrl.append(redirectSuccessURL);
                resultUrl.append("?rc=" + verify.getRc());
                resultUrl.append("&tranID=" + verify.getTranID());
                resultUrl.append("&enrStatus=" + verify.getEnrStatus());
                resultUrl.append("&authStatus=" + verify.getAuthStatus());
                resultUrl.append("&shopID=" +orderId);
                resultUrl.append("&orderId=" +orderId);

                Debug.logWarning("### Save tranID ["+verify.getTranID()+"] and authStatus ["+verify.getAuthStatus()+"] from IgfsCgVerify object ###",MODULE);
                logger.logInfo("### Save tranID ["+verify.getTranID()+"] and authStatus ["+verify.getAuthStatus()+"] from IgfsCgVerify object ###");

                Long tranID = verify.getTranID();
                String authStatus = verify.getAuthStatus();

                boolean ordHeadUpdated = UnicreditWorker.updateOrderHeader(delegator, orderId, null,tranID, authStatus, null);

                Debug.logWarning("### OrderHeader updated with this status: ["+ordHeadUpdated+"]. Redirect to successUrl... ###",MODULE);
                logger.logInfo("### OrderHeader updated with this status: ["+ordHeadUpdated+"]. Redirect to successUrl... ###");

                boolean isApproved = UnicreditWorker.approveOrder(dispatcher, systemUserLogin, orderId, productStore);

                String rcDesc = UnicreditHelper.findDescriptionByRcOutcome(verify.getRc());

                String msg = "Order status approved ["+isApproved+"] with this outcome: "+verify.getRc()+"-"+rcDesc+ "and transaction ID: "+verify.getTranID();

                UnicreditWorker.createOrderHeaderNote(dispatcher, orderId, msg, username, password);

                // call the email confirm service
                //Map<String, String> emailContext = UtilMisc.toMap("orderId", orderId);

                //dispatcher.runSync("sendOrderConfirmation", emailContext);

                Debug.logWarning("### Redirect to success URL ["+resultUrl +"]... ###",MODULE);

                response.sendRedirect(resultUrl.toString());
            }

        //} catch (GenericServiceException e) {
          //  Debug.logError(e, "Problems sending email confirmation", MODULE);
        } catch (IgfsException e) {
            // TODO Auto-generated catch block
            Debug.logError(e.getMessage(), MODULE);
            return "error";
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            Debug.logError(e.getMessage(), MODULE);
            return "error";
        } catch (IOException e) {
            // TODO Auto-generated catch block
            Debug.logError(e.getMessage(), MODULE);
            return "error";
        }

        return "success";
    }

} //end class
