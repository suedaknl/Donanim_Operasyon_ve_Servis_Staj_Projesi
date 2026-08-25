const {setGlobalOptions} = require("firebase-functions/v2");
const {
  onDocumentUpdated,
} = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");
const logger = require("firebase-functions/logger");

admin.initializeApp();

setGlobalOptions({maxInstances: 10});

exports.onServiceAssignedDirectly = onDocumentUpdated(
    "services/{serviceId}",
    async (event) => {
      const serviceId = event.params.serviceId;
      const beforeData = event.data.before.data();
      const afterData = event.data.after.data();

      logger.info(
          `SERVICE_ASSIGNED triggered serviceId=${serviceId}`,
      );

      if (afterData.assignmentType !== "DIRECT") {
        logger.info(
            `SERVICE_ASSIGNED skipped ` +
            `reason=not_direct_assignment serviceId=${serviceId}`,
        );
        return;
      }

      const assignedUid = afterData.assignedPersonnelUid;
      if (
        !assignedUid ||
        typeof assignedUid !== "string" ||
        assignedUid.trim() === ""
      ) {
        logger.info(
            `SERVICE_ASSIGNED skipped ` +
            `reason=empty_assigned_personnel_uid serviceId=${serviceId}`,
        );
        return;
      }

      const beforeUid = beforeData.assignedPersonnelUid;
      if (beforeUid === assignedUid) {
        logger.info(
            `SERVICE_ASSIGNED skipped ` +
            `reason=personnel_uid_not_changed serviceId=${serviceId}`,
        );
        return;
      }

      if (
        beforeData.assignmentType === "POOL" &&
        afterData.assignmentType === "DIRECT"
      ) {
        logger.info(
            `SERVICE_ASSIGNED skipped ` +
            `reason=pool_to_direct_claim serviceId=${serviceId}`,
        );
        return;
      }

      try {
        const userDocRef = admin
            .firestore()
            .collection("notification_users")
            .doc(assignedUid);
        const userDoc = await userDocRef.get();

        if (!userDoc.exists) {
          logger.info(
              `SERVICE_ASSIGNED skipped ` +
              `reason=user_doc_not_found serviceId=${serviceId}`,
          );
          return;
        }

        const userData = userDoc.data();
        const fcmToken = userData ? userData.fcmToken : null;

        if (
          !fcmToken ||
          typeof fcmToken !== "string" ||
          fcmToken.trim() === ""
        ) {
          logger.info(
              `SERVICE_ASSIGNED skipped ` +
              `reason=fcm_token_empty_or_missing serviceId=${serviceId}`,
          );
          return;
        }

        const title = "Yeni İş Emri";
        const body = "Size yeni bir iş emri atandı.";

        const message = {
          token: fcmToken,
          notification: {
            title: title,
            body: body,
          },
          data: {
            type: "SERVICE_ASSIGNED",
            targetId: String(serviceId),
            title: title,
            body: body,
          },
        };

        await admin.messaging().send(message);
        logger.info(
            `SERVICE_ASSIGNED sent serviceId=${serviceId}`,
        );
      } catch (error) {
        logger.error(
            `SERVICE_ASSIGNED error serviceId=${serviceId}`,
            error,
        );
      }
    },
);
