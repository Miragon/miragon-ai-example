import { useMemo } from "react";
import { Controller, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  Button,
  Field,
  Input,
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/shared/ui";
import { useCopy } from "@/shared/i18n";
import { useListBikes } from "@/entities/bike";
import { useSubmitLeasingRequestAction } from "../api/use-submit-leasing-request";
import { makeSubmitFormSchema, type SubmitFormValues } from "../model/submit-form-schema";

function BikeSelect({
  bikes,
  value,
  onValueChange,
  placeholder,
  ...fieldControlProps
}: {
  bikes: { bikeId: string; model: string }[];
  value: string;
  onValueChange: (bikeId: string) => void;
  placeholder: string;
  id?: string;
  "aria-invalid"?: boolean;
  "aria-describedby"?: string;
}) {
  return (
    <Select value={value || undefined} onValueChange={onValueChange}>
      <SelectTrigger {...fieldControlProps}>
        <SelectValue placeholder={placeholder} />
      </SelectTrigger>
      <SelectContent>
        {bikes.map((bike) => (
          <SelectItem key={bike.bikeId} value={bike.bikeId}>
            {bike.model}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}

export function SubmitLeasingForm({ onCreated }: { onCreated: (applicationId: string) => void }) {
  const copy = useCopy();
  const bikes = useListBikes();

  const schema = useMemo(() => makeSubmitFormSchema(copy), [copy]);

  const {
    register,
    handleSubmit,
    control,
    formState: { errors },
  } = useForm<SubmitFormValues>({
    resolver: zodResolver(schema),
    defaultValues: { customerName: "", email: "", bikeId: "" },
  });

  const submit = useSubmitLeasingRequestAction(onCreated);

  const onSubmit = handleSubmit((values) => submit.mutate({ data: values }));

  const bikeList = bikes.data ?? [];

  return (
    <form onSubmit={onSubmit} className="flex max-w-xl flex-col gap-5" noValidate>
      <Field label={copy.submit.customerName} required error={errors.customerName?.message}>
        <Input {...register("customerName")} autoComplete="name" />
      </Field>

      <Field label={copy.submit.email} required error={errors.email?.message}>
        <Input type="email" {...register("email")} autoComplete="email" />
      </Field>

      <div className="grid grid-cols-2 gap-4">
        <Field label={copy.submit.age} required error={errors.age?.message}>
          <Input type="number" inputMode="numeric" {...register("age", { valueAsNumber: true })} />
        </Field>
        <Field
          label={copy.submit.monthlyNetIncome}
          required
          error={errors.monthlyNetIncome?.message}
        >
          <Input
            type="number"
            inputMode="numeric"
            {...register("monthlyNetIncome", { valueAsNumber: true })}
          />
        </Field>
      </div>

      <Controller
        control={control}
        name="bikeId"
        render={({ field }) => (
          <Field label={copy.submit.bike} required error={errors.bikeId?.message}>
            <BikeSelect
              bikes={bikeList}
              value={field.value}
              onValueChange={field.onChange}
              placeholder={copy.submit.bikePlaceholder}
            />
          </Field>
        )}
      />

      <div>
        <Button type="submit" disabled={submit.isPending}>
          {submit.isPending ? copy.submit.submitting : copy.submit.submit}
        </Button>
      </div>
    </form>
  );
}
