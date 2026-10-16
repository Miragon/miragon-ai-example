import { z } from "zod";
import { SubmitLeasingRequestBody } from "@/shared/api/generated/zod";
import type { Copy } from "@/shared/i18n";

/**
 * Reuses the generated request-body schema so the form and the wire format cannot drift, and only
 * adds localized shape messages. Never add `age >= 18` or a bike-catalogue check: those are the
 * process's "Validate application" rule, and duplicating them here would make the rejection path
 * unreachable from the UI. `monthlyNetIncome > 0` is an input rule the API itself answers with 400.
 */
export function makeSubmitFormSchema(copy: Copy) {
  return SubmitLeasingRequestBody.extend({
    customerName: z.string().min(1, copy.submit.validationName),
    email: z.string().min(1, copy.submit.validationEmail).email(copy.submit.validationEmail),
    age: z.number({ message: copy.submit.validationAge }).int(copy.submit.validationAge),
    monthlyNetIncome: z
      .number({ message: copy.submit.validationIncome })
      .positive(copy.submit.validationIncomePositive),
    bikeId: z.string().min(1, copy.submit.validationBike),
  });
}

export type SubmitFormValues = z.infer<ReturnType<typeof makeSubmitFormSchema>>;
