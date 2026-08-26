const {setGlobalOptions} = require("firebase-functions/v2");
const {
  onDocumentUpdated,
  onDocumentCreated,
} = require("firebase-functions/v2/firestore");
const {
  onCall,
  HttpsError,
} = require("firebase-functions/v2/https");
const {defineSecret} = require("firebase-functions/params");

const admin = require("firebase-admin");
const logger = require("firebase-functions/logger");

const GEMINI_API_KEY = defineSecret("GEMINI_API_KEY");
const GROQ_API_KEY = defineSecret("GROQ_API_KEY");

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
          const adminUid = doc.id;

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
          const personnelUid = doc.id;

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

// ============================================================
// AI ASSISTANT
// Gemini -> Groq fallback
// ============================================================

exports.askAiAssistant = onCall(
    {
      region: "europe-west1",
      secrets: [GEMINI_API_KEY, GROQ_API_KEY],
      timeoutSeconds: 60,
      memory: "256MiB",
    },
    async (request) => {
      if (!request.auth) {
        throw new HttpsError(
            "unauthenticated",
            "AI Asistanı kullanmak için giriş yapmalısınız.",
        );
      }

      const data = request.data || {};

      const message =
        typeof data.message === "string" ? data.message.trim() : "";

      const role =
        typeof data.role === "string" ?
          data.role.trim().toUpperCase() :
          "";

      const history = Array.isArray(data.history) ?
        data.history.slice(-12) :
        [];

      const context =
        typeof data.context === "string" ?
          data.context.trim() :
          "";

      if (!message) {
        throw new HttpsError(
            "invalid-argument",
            "Mesaj boş olamaz.",
        );
      }

      if (message.length > 4000) {
        throw new HttpsError(
            "invalid-argument",
            "Mesaj çok uzun.",
        );
      }

      if (!["ADMIN", "PERSONNEL"].includes(role)) {
        throw new HttpsError(
            "invalid-argument",
            "Geçersiz kullanıcı rolü.",
        );
      }

      const systemPrompt = createAiSystemPrompt(role, context);

      try {
        const geminiAnswer = await askGemini(
            systemPrompt,
            history,
            message,
        );

        if (geminiAnswer) {
          return {
            answer: geminiAnswer,
            provider: "gemini",
          };
        }
      } catch (error) {
        console.error(
            "Gemini AI hatası:",
            (error && error.message) || error,
        );
      }

      try {
        const groqAnswer = await askGroq(
            systemPrompt,
            history,
            message,
        );

        if (groqAnswer) {
          return {
            answer: groqAnswer,
            provider: "groq",
          };
        }
      } catch (error) {
        console.error(
            "Groq AI hatası:",
            (error && error.message) || error,
        );
      }

      throw new HttpsError(
          "unavailable",
          "AI servislerine şu anda ulaşılamıyor.",
      );
    },
);

/**
 * Creates the AI system prompt based on user role and context.
 * @param {string} role - The user role (ADMIN or PERSONNEL).
 * @param {string} context - Optional application context data.
 * @return {string} The constructed system prompt.
 */
function createAiSystemPrompt(role, context) {
  let prompt = "";

  if (role === "ADMIN") {
    prompt = `
Sen bir Donanım Operasyon ve Servis Yönetim AI Asistanısın.

Kullanıcı ADMIN rolündedir.

Görevin yalnızca verileri tekrar etmek değildir.
Yöneticiye karar desteği sağlamalısın.

Özellikle:
- bekleyen iş emirlerini değerlendir,
- öncelik seviyelerini dikkate al,
- uzun süredir bekleyen işleri fark et,
- kritik işleri öne çıkar,
- personel iş yükünü değerlendir,
- gecikme risklerini belirt,
- operasyonel sorunları tespit et,
- hangi işe neden önce müdahale edilmesi gerektiğini açıkla,
- kısa ve uygulanabilir öneriler sun.

Veri yoksa veri varmış gibi davranma.
Bilmediğin operasyonel bilgileri uydurma.

Kullanıcı genel bir soru sorarsa normal şekilde yardımcı ol.
Yanıtlarını Türkçe, açık ve kısa tut.
`;
  } else {
    prompt = `
Sen bir Donanım Operasyon ve Servis Saha AI Asistanısın.

Kullanıcı PERSONEL rolündedir.

Görevin saha personeline operasyon sırasında yardımcı olmaktır.

Özellikle:
- atanmış işleri anlamasına yardımcı ol,
- arıza açıklamalarını yorumla,
- servis sürecində izlenebilecek adımları öner,
- servis notlarının daha anlaşılır yazılmasına yardımcı ol,
- görev önceliklendirmesinde destek ol,
- kullanıcıya kısa ve uygulanabilir öneriler sun.

Tehlikeli veya emin olmadığın teknik işlemleri kesin bilgi gibi verme.
Veri yoksa veri varmış gibi davranma.
Bilmediğin bilgileri uydurma.

Yanıtlarını Türkçe, açık ve kısa tut.
`;
  }

  if (context) {
    prompt += `

Uygulamadan sağlanan güncel bağlam:

${context}

Bu bağlamı yalnızca gerektiğinde kullan.
Bağlamda olmayan bilgileri uydurma.
`;
  }

  prompt += `

Yanıt kuralı:
Yanıtı tamamlamadan kesme.
Gereksiz uzun girişlerden kaçın.
Önce doğrudan sonucu ver, ardından kısa gerekçeler sun.
Ağır markdown biçimlendirmesi kullanma.
Başlıkları sade metin olarak yaz.
### ve ** gibi markdown işaretlerini kullanma.
`;

  return prompt.trim();
}

/**
 * Calls Gemini API to generate a response.
 * @param {string} systemPrompt - The system prompt.
 * @param {Array} history - The chat conversation history.
 * @param {string} message - The current user message.
 * @return {Promise<string>} The AI response text.
 */
async function askGemini(systemPrompt, history, message) {
  const apiKey = GEMINI_API_KEY.value();

  const contents = [];

  for (const item of history) {
    if (!item || typeof item.content !== "string") {
      continue;
    }

    const itemRole =
      item.role === "assistant" ? "model" : "user";

    contents.push({
      role: itemRole,
      parts: [
        {
          text: item.content,
        },
      ],
    });
  }

  contents.push({
    role: "user",
    parts: [
      {
        text: message,
      },
    ],
  });

  const response = await fetch(
      "https://generativelanguage.googleapis.com/" +
      "v1beta/models/gemini-3.6-flash:generateContent" +
      `?key=${apiKey}`,
      {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          systemInstruction: {
            parts: [
              {
                text: systemPrompt,
              },
            ],
          },
          contents,
          generationConfig: {
            temperature: 0.4,
            maxOutputTokens: 2500,
            thinkingConfig: {
              thinkingLevel: "low",
            },
          },
        }),
      },
  );

  if (!response.ok) {
    const errorText = await response.text();

    throw new Error(
        `Gemini HTTP ${response.status}: ${errorText}`,
    );
  }

  const data = await response.json();

  let finishReason = "UNKNOWN";
  if (
    data &&
    data.candidates &&
    data.candidates[0] &&
    data.candidates[0].finishReason
  ) {
    finishReason = data.candidates[0].finishReason;
  }
  logger.info(`GEMINI finishReason=${finishReason}`);

  const parts =
    data &&
    data.candidates &&
    data.candidates[0] &&
    data.candidates[0].content &&
    data.candidates[0].content.parts ?
      data.candidates[0].content.parts :
      [];

  let answer = "";
  for (let i = 0; i < parts.length; i++) {
    const part = parts[i];
    if (part && part.text) {
      answer += part.text;
    }
  }
  answer = answer.trim();

  if (!answer) {
    throw new Error("Gemini boş cevap döndürdü.");
  }

  return answer;
}

/**
 * Calls Groq API to generate a response.
 * @param {string} systemPrompt - The system prompt.
 * @param {Array} history - The chat conversation history.
 * @param {string} message - The current user message.
 * @return {Promise<string>} The AI response text.
 */
async function askGroq(systemPrompt, history, message) {
  const apiKey = GROQ_API_KEY.value();

  const messages = [
    {
      role: "system",
      content: systemPrompt,
    },
  ];

  for (const item of history) {
    if (!item || typeof item.content !== "string") {
      continue;
    }

    messages.push({
      role:
        item.role === "assistant" ?
          "assistant" :
          "user",
      content: item.content,
    });
  }

  messages.push({
    role: "user",
    content: message,
  });

  const response = await fetch(
      "https://api.groq.com/openai/v1/chat/completions",
      {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${apiKey}`,
        },
        body: JSON.stringify({
          model: "openai/gpt-oss-20b",
          messages,
          temperature: 0.4,
          max_tokens: 2000,
        }),
      },
  );

  if (!response.ok) {
    const errorText = await response.text();

    throw new Error(
        `Groq HTTP ${response.status}: ${errorText}`,
    );
  }

  const data = await response.json();

  let finishReason = "UNKNOWN";
  if (
    data &&
    data.choices &&
    data.choices[0] &&
    data.choices[0].finish_reason
  ) {
    finishReason = data.choices[0].finish_reason;
  }
  logger.info(`GROQ finishReason=${finishReason}`);

  const answer =
    data &&
    data.choices &&
    data.choices[0] &&
    data.choices[0].message &&
    data.choices[0].message.content ?
      data.choices[0].message.content.trim() :
      "";

  if (!answer) {
    throw new Error("Groq boş cevap döndürdü.");
  }

  return answer;
}
