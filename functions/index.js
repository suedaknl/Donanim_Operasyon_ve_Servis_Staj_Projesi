const {setGlobalOptions} = require("firebase-functions/v2");
const {
  onDocumentUpdated,
  onDocumentCreated,
} = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");
const logger = require("firebase-functions/logger");

admin.initializeApp();

setGlobalOptions({maxInstances: 10});

/**
 * Helper to create a notification record in Firestore.
 * @param {Object} params - The notification parameters.
 * @param {string} params.recipientUid - The recipient user UID.
 * @param {string} params.role - The recipient role (ADMIN or PERSONNEL).
 * @param {string} params.type - The notification type.
 * @param {string} params.title - The notification title.
 * @param {string} params.body - The notification body.
 * @param {string} [params.targetId] - The optional target ID.
 * @return {Promise<void>}
 */
async function createNotificationRecord({
  recipientUid,
  role,
  type,
  title,
  body,
  targetId,
}) {
  try {
    if (!recipientUid) return;
    await admin.firestore().collection("notifications").add({
      recipientUid: recipientUid,
      role: role,
      type: type,
      title: title || "",
      body: body || "",
      targetId: targetId || null,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      isRead: false,
    });
  } catch (error) {
    logger.error("Error creating notification history record:", error);
  }
}

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

      const title = "Yeni İş Emri";
      const body = "Size yeni bir iş emri atandı.";

      // Firestore Bildirim Merkezi Geçmişine Kayıt Ekleme
      await createNotificationRecord({
        recipientUid: assignedUid,
        role: "PERSONNEL",
        type: "SERVICE_ASSIGNED",
        title: title,
        body: body,
        targetId: String(serviceId),
      });

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

exports.onLeaveRequestCreated = onDocumentCreated(
    "leave_requests/{leaveId}",
    async (event) => {
      const leaveId = event.params.leaveId;
      logger.info(
          `LEAVE_REQUEST_CREATED triggered leaveId=${leaveId}`,
      );

      try {
        const adminsSnapshot = await admin
            .firestore()
            .collection("notification_users")
            .where("role", "==", "ADMIN")
            .get();

        if (adminsSnapshot.empty) {
          logger.info(
              `LEAVE_REQUEST_CREATED skipped ` +
              `reason=no_admin_users_found leaveId=${leaveId}`,
          );
          return;
        }

        const title = "Yeni İzin Talebi";
        const body = "Yeni bir personel izin talebi oluşturdu.";

        const sendPromises = adminsSnapshot.docs.map(async (doc) => {
          const adminData = doc.data();
          const adminUid = doc.id; // Admin Firebase UID

          // Firestore Bildirim Merkezi Geçmişine Kayıt Ekleme
          await createNotificationRecord({
            recipientUid: adminUid,
            role: "ADMIN",
            type: "LEAVE_REQUEST_CREATED",
            title: title,
            body: body,
            targetId: String(leaveId),
          });

          const fcmToken = adminData ? adminData.fcmToken : null;

          if (
            !fcmToken ||
            typeof fcmToken !== "string" ||
            fcmToken.trim() === ""
          ) {
            return;
          }

          const message = {
            token: fcmToken,
            notification: {
              title: title,
              body: body,
            },
            data: {
              type: "LEAVE_REQUEST_CREATED",
              targetId: String(leaveId),
              title: title,
              body: body,
            },
          };

          try {
            await admin.messaging().send(message);
            logger.info(
                `LEAVE_REQUEST_CREATED sent to admin leaveId=${leaveId}`,
            );
          } catch (err) {
            logger.error(
                `LEAVE_REQUEST_CREATED error sending to an admin ` +
                `leaveId=${leaveId}`,
                err,
            );
          }
        });

        await Promise.all(sendPromises);
      } catch (error) {
        logger.error(
            `LEAVE_REQUEST_CREATED error leaveId=${leaveId}`,
            error,
        );
      }
    },
);

exports.onLeaveRequestUpdated = onDocumentUpdated(
    "leave_requests/{leaveId}",
    async (event) => {
      const leaveId = event.params.leaveId;
      const beforeData = event.data.before.data();
      const afterData = event.data.after.data();

      if (!beforeData || !afterData) {
        return;
      }

      if (beforeData.status === afterData.status) {
        return;
      }

      let title = "";
      let body = "";
      let type = "";

      if (afterData.status === "APPROVED") {
        title = "İzin Onaylandı";
        body = "İzin talebiniz onaylandı.";
        type = "LEAVE_APPROVED";
      } else if (afterData.status === "REJECTED") {
        title = "İzin Reddedildi";
        body = "İzin talebiniz reddedildi.";
        type = "LEAVE_REJECTED";
      } else {
        return;
      }

      logger.info(
          `LEAVE_STATUS triggered ` +
          `leaveId=${leaveId} status=${afterData.status}`,
      );

      const personnelId = afterData.personnelId;
      if (personnelId === undefined || personnelId === null) {
        logger.info(
            `LEAVE_STATUS skipped ` +
            `reason=no_personnel_id leaveId=${leaveId}`,
        );
        return;
      }

      try {
        const usersSnapshot = await admin
            .firestore()
            .collection("notification_users")
            .where("personnelId", "==", Number(personnelId))
            .get();

        if (usersSnapshot.empty) {
          logger.info(
              `LEAVE_STATUS skipped ` +
              `reason=user_doc_not_found leaveId=${leaveId}`,
          );
          return;
        }

        const sendPromises = usersSnapshot.docs.map(async (doc) => {
          const userData = doc.data();
          const personnelUid = doc.id; // Personel Firebase UID

          // Firestore Bildirim Merkezi Geçmişine Kayıt Ekleme
          await createNotificationRecord({
            recipientUid: personnelUid,
            role: "PERSONNEL",
            type: type,
            title: title,
            body: body,
            targetId: String(leaveId),
          });

          const fcmToken = userData ? userData.fcmToken : null;

          if (
            !fcmToken ||
            typeof fcmToken !== "string" ||
            fcmToken.trim() === ""
          ) {
            return;
          }

          const message = {
            token: fcmToken,
            notification: {
              title: title,
              body: body,
            },
            data: {
              type: type,
              targetId: String(leaveId),
              title: title,
              body: body,
            },
          };

          try {
            await admin.messaging().send(message);
            logger.info(
                `LEAVE_STATUS sent ` +
                `type=${type} leaveId=${leaveId}`,
            );
          } catch (err) {
            logger.error(
                `LEAVE_STATUS error ` +
                `leaveId=${leaveId}`,
                err,
            );
          }
        });

        await Promise.all(sendPromises);
      } catch (error) {
        logger.error(
            `LEAVE_STATUS error leaveId=${leaveId}`,
            error,
        );
      }
    },
);

/**
 * Triggers when a new shift document is created.
 * Sends FCM notification and logs notification history to Firestore.
 */
exports.onShiftCreated = onDocumentCreated(
    "shifts/{shiftId}",
    async (event) => {
      const shiftId = event.params.shiftId;
      const afterData = event.data.data();

      if (!afterData) {
        return;
      }

      logger.info(`SHIFT_CREATED triggered shiftId=${shiftId}`);

      const personnelId = afterData.personnelId;
      if (personnelId === undefined || personnelId === null) {
        logger.info(
            `SHIFT_CREATED skipped reason=no_personnel_id shiftId=${shiftId}`,
        );
        return;
      }

      try {
        const usersSnapshot = await admin
            .firestore()
            .collection("notification_users")
            .where("personnelId", "==", Number(personnelId))
            .get();

        if (usersSnapshot.empty) {
          logger.info(
              `SHIFT_CREATED skipped reason=user_not_found shiftId=${shiftId}`,
          );
          return;
        }

        const title = "Yeni Vardiya";
        const body = "Size yeni bir vardiya atandı.";
        const type = "SHIFT_CREATED";

        const sendPromises = usersSnapshot.docs.map(async (doc) => {
          const userData = doc.data();
          const personnelUid = doc.id;

          await createNotificationRecord({
            recipientUid: personnelUid,
            role: "PERSONNEL",
            type: type,
            title: title,
            body: body,
            targetId: String(shiftId),
          });

          const fcmToken = userData ? userData.fcmToken : null;
          if (
            !fcmToken ||
            typeof fcmToken !== "string" ||
            fcmToken.trim() === ""
          ) {
            return;
          }

          const message = {
            token: fcmToken,
            notification: {
              title: title,
              body: body,
            },
            data: {
              type: type,
              targetId: String(shiftId),
              title: title,
              body: body,
            },
          };

          try {
            await admin.messaging().send(message);
            logger.info(`SHIFT_CREATED sent shiftId=${shiftId}`);
          } catch (err) {
            logger.error(`SHIFT_CREATED error shiftId=${shiftId}`, err);
          }
        });

        await Promise.all(sendPromises);
      } catch (error) {
        logger.error(`SHIFT_CREATED error shiftId=${shiftId}`, error);
      }
    },
);

exports.onShiftUpdated = onDocumentUpdated(
    "shifts/{shiftId}",
    async (event) => {
      const shiftId = event.params.shiftId;
      const beforeData = event.data.before.data();
      const afterData = event.data.after.data();

      if (!beforeData || !afterData) {
        return;
      }

      // CANCELLED geçişini ayrı olarak yakalıyoruz (Vardiya İptal Edildi)
      const isNewlyCancelled =
        beforeData.status !== "CANCELLED" && afterData.status === "CANCELLED";

      if (isNewlyCancelled) {
        logger.info(`SHIFT_CANCELLED triggered shiftId=${shiftId}`);

        const personnelId = afterData.personnelId;
        if (personnelId === undefined || personnelId === null) {
          return;
        }

        try {
          const usersSnapshot = await admin
              .firestore()
              .collection("notification_users")
              .where("personnelId", "==", Number(personnelId))
              .get();

          if (usersSnapshot.empty) {
            return;
          }

          const title = "Vardiyanız İptal Edildi";
          const body = "Planlanan vardiyanız iptal edildi.";
          const type = "SHIFT_CANCELLED";

          const sendPromises = usersSnapshot.docs.map(async (doc) => {
            const userData = doc.data();
            const personnelUid = doc.id;

            // Firestore Bildirim Merkezi Geçmişine Kayıt Ekleme
            await createNotificationRecord({
              recipientUid: personnelUid,
              role: "PERSONNEL",
              type: type,
              title: title,
              body: body,
              targetId: String(shiftId),
            });

            const fcmToken = userData ? userData.fcmToken : null;
            if (
              !fcmToken ||
              typeof fcmToken !== "string" ||
              fcmToken.trim() === ""
            ) {
              return;
            }

            const message = {
              token: fcmToken,
              notification: {
                title: title,
                body: body,
              },
              data: {
                type: type,
                targetId: String(shiftId),
                title: title,
                body: body,
              },
            };

            try {
              await admin.messaging().send(message);
              logger.info(`SHIFT_CANCELLED sent shiftId=${shiftId}`);
            } catch (err) {
              logger.error(`SHIFT_CANCELLED error shiftId=${shiftId}`, err);
            }
          });

          await Promise.all(sendPromises);
        } catch (error) {
          logger.error(`SHIFT_CANCELLED error shiftId=${shiftId}`, error);
        }
        return;
      }

      const isDateChanged = beforeData.shiftDate !== afterData.shiftDate;
      const isStartChanged = beforeData.startTime !== afterData.startTime;
      const isEndChanged = beforeData.endTime !== afterData.endTime;
      const isStatusChanged = beforeData.status !== afterData.status;

      if (
        !isDateChanged &&
        !isStartChanged &&
        !isEndChanged &&
        !isStatusChanged
      ) {
        return;
      }

      if (afterData.status === "CANCELLED") {
        return;
      }

      logger.info(
          `SHIFT_UPDATED triggered shiftId=${shiftId}`,
      );

      const personnelId = afterData.personnelId;
      if (personnelId === undefined || personnelId === null) {
        logger.info(
            `SHIFT_UPDATED skipped ` +
            `reason=no_personnel_id shiftId=${shiftId}`,
        );
        return;
      }

      try {
        const usersSnapshot = await admin
            .firestore()
            .collection("notification_users")
            .where("personnelId", "==", Number(personnelId))
            .get();

        if (usersSnapshot.empty) {
          logger.info(
              `SHIFT_UPDATED skipped ` +
              `reason=user_doc_not_found shiftId=${shiftId}`,
          );
          return;
        }

        const title = "Vardiyanız Güncellendi";
        const body = "Vardiya bilgileriniz güncellendi.";
        const type = "SHIFT_UPDATED";

        const sendPromises = usersSnapshot.docs.map(async (doc) => {
          const userData = doc.data();
          const personnelUid = doc.id; // Personel Firebase UID

          // Firestore Bildirim Merkezi Geçmişine Kayıt Ekleme
          await createNotificationRecord({
            recipientUid: personnelUid,
            role: "PERSONNEL",
            type: type,
            title: title,
            body: body,
            targetId: String(shiftId),
          });

          const fcmToken = userData ? userData.fcmToken : null;

          if (
            !fcmToken ||
            typeof fcmToken !== "string" ||
            fcmToken.trim() === ""
          ) {
            return;
          }

          const message = {
            token: fcmToken,
            notification: {
              title: title,
              body: body,
            },
            data: {
              type: type,
              targetId: String(shiftId),
              title: title,
              body: body,
            },
          };

          try {
            await admin.messaging().send(message);
            logger.info(
                `SHIFT_UPDATED sent shiftId=${shiftId}`,
            );
          } catch (err) {
            logger.error(
                `SHIFT_UPDATED error ` +
                `shiftId=${shiftId}`,
                err,
            );
          }
        });

        await Promise.all(sendPromises);
      } catch (error) {
        logger.error(
            `SHIFT_UPDATED error shiftId=${shiftId}`,
            error,
        );
      }
    },
);
