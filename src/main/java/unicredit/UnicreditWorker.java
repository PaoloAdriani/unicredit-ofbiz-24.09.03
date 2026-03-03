package unicredit;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.transaction.GenericTransactionException;
import org.apache.ofbiz.entity.transaction.TransactionUtil;
import org.apache.ofbiz.order.order.OrderChangeHelper;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

import java.util.HashMap;
import java.util.Map;

public class UnicreditWorker {

    public static final String MODULE = UnicreditWorker.class.getName();

    public static GenericValue getSystemUserLogin(Delegator delegator) {

        GenericValue systemmUserLogin = null;

        try {
            systemmUserLogin = delegator.findOne("UserLogin", UtilMisc.toMap("userLoginId", "system"), false);
        } catch (GenericEntityException gee) {
            Debug.logError("Error in retrieving system user login record.", MODULE);
            return null;
        }

        return systemmUserLogin;

    }

    public static GenericValue findOrderHeaderFromOrderId(Delegator delegator, String orderId)
    {
        GenericValue orderHeader = null;

        try {
            orderHeader = delegator.findOne("OrderHeader", UtilMisc.toMap("orderId", orderId), false);

        } catch (GenericEntityException e) {
            // TODO Auto-generated catch block
            String msg = "Error to retrieve orderHeader from order [" + orderId+ "]. Error is => " + e.getMessage();
            Debug.logError(msg, MODULE);
        }

        return orderHeader;
    }

    public static String findMpPaymentIdFromOrderId(Delegator delegator, String orderId) {
        String orderPaymentID = null;

        GenericValue orderHeader = null;

        try {
            orderHeader = delegator.findOne("OrderHeader", UtilMisc.toMap("orderId", orderId), false);

        } catch (GenericEntityException e) {
            // TODO Auto-generated catch block
            String msg = "Error to retrieve orderHeader from order [" + orderId+ "]. Error is => " + e.getMessage();
            Debug.logError(msg, MODULE);
            return null;
        }

        if (orderHeader != null && !orderHeader.isEmpty()) {
            orderPaymentID = orderHeader.getString("mpPaymentId");
        }

        return orderPaymentID;
    }

    public static boolean createOrderHeaderNote(LocalDispatcher dispatcher, String orderId, String noteMsg,
                                                String username, String password) {

        boolean noteCreated = false;

        Map<String, Object> inMap = new HashMap<>();
        inMap.put("internalNote", "Y");
        inMap.put("orderId", orderId);
        inMap.put("note", noteMsg);
        inMap.put("login.username", username);
        inMap.put("login.password", password);

        Map<String, Object> outMap = null;

        try {
            outMap = dispatcher.runSync("createOrderNote", inMap);
        } catch (GenericServiceException gse) {
            Debug.logError(gse.getMessage(), MODULE);
            return false;
        }

        if (ServiceUtil.isSuccess(outMap)) {
            noteCreated = true;
        }

        return noteCreated;

    }

    /* ############## ORDER CHANGE METHODS ############# */
    public static boolean approveOrder(LocalDispatcher dispatcher, GenericValue userLogin, String orderId,
                                       GenericValue productStore) {

        // Approve the order if store setting for auto-approving an order is "Y"
        boolean orderApproved = false;

        boolean autoApproveOrder = (UtilValidate.isEmpty(productStore.get("autoApproveOrder"))
                || "Y".equalsIgnoreCase(productStore.getString("autoApproveOrder")));

        if (autoApproveOrder) {
            orderApproved = OrderChangeHelper.approveOrder(dispatcher, userLogin, orderId, false);
        }

        return orderApproved;

    }

    public static boolean cancelOrder(LocalDispatcher dispatcher, GenericValue userLogin, String orderId) {

        boolean orderCancelled = false;

        // attempt to start a transaction
        boolean beganTransaction = false;
        try {
            beganTransaction = TransactionUtil.begin();

        } catch (GenericTransactionException gte) {
            Debug.logError(gte, "Unable to begin transaction", MODULE);
        }

        // cancel the order
        boolean okay = OrderChangeHelper.cancelOrder(dispatcher, userLogin, orderId);

        if (okay) {
            try {
                TransactionUtil.commit(beganTransaction);
            } catch (GenericTransactionException gte) {
                Debug.logError(gte, "Unable to commit transaction", MODULE);
            }
        } else {
            try {
                TransactionUtil.rollback(beganTransaction, "Failure in processing MultiSafepay cancel callback", null);
            } catch (GenericTransactionException gte) {
                Debug.logError(gte, "Unable to rollback transaction", MODULE);
            }
        }

        orderCancelled = okay;

        return orderCancelled;
    }

    public static boolean updateOrderHeader(Delegator delegator, String orderId, String paymentID, Long tranID, String authStatus, Long orderAmount) {

        boolean result = false;

        GenericValue orderHeader = null;

        try {
            orderHeader = delegator.findOne("OrderHeader", UtilMisc.toMap("orderId", orderId), false);

            if(orderAmount != null)
            {
                orderHeader.put("mpAmount", orderAmount);
            }

            if(paymentID != null)
            {
                orderHeader.put("mpPaymentId", paymentID);
            }

            if(tranID != null)
            {
                orderHeader.put("mpTransId", tranID);
            }

            if(authStatus != null)
            {
                orderHeader.put("mpAuthStatus", authStatus);
            }

            orderHeader.store();

            result = true;

        } catch (GenericEntityException e) {
            // TODO Auto-generated catch block
            String msg = "Error to storing value into orderHeader for order [" + orderId+ "]. Error is => " + e.getMessage();
            Debug.logError(msg, MODULE);
        }

        return result;

    }

} //end class
