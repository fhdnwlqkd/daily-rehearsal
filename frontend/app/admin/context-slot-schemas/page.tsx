"use client";

import { useEffect, useMemo, useState } from "react";
import { AlertCircle, CheckCircle2, Database, RefreshCw } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { cn } from "@/lib/utils";

const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

type ApiResponse<T> = {
  success: boolean;
  data: T | null;
  error: {
    code: string;
    name: string;
    message: string;
  } | null;
};

type SchemaKeysResponse = {
  schemaKeys: string[];
};

type RequiredLevel = "REQUIRED" | "SOFT_REQUIRED" | "OPTIONAL";
type SlotType = "TEXT" | "SINGLE_SELECT";

type ContextSlotOption = {
  id: number;
  optionKey: string;
  label: string;
};

type ContextSlot = {
  id: number;
  slotKey: string;
  label: string;
  slotType: SlotType;
  extractionHint?: string;
  followUpHint?: string;
  defaultLiteralValue?: string;
  defaultOption?: ContextSlotOption;
  options: ContextSlotOption[];
};

type ContextSlotSchemaItem = {
  id: number;
  requiredLevel: RequiredLevel;
  priority: number;
  active: boolean;
  slot: ContextSlot;
};

type ContextSlotSchemaResponse = {
  id: number;
  schemaKey: string;
  name: string;
  maxFollowUpAttempt: number;
  active: boolean;
  items: ContextSlotSchemaItem[];
};

async function getApiData<T>(path: string): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: { Accept: "application/json" },
    cache: "no-store",
  });

  if (!response.ok) {
    throw new Error(`Request failed with ${response.status}`);
  }

  const body = (await response.json()) as ApiResponse<T>;
  if (!body.success || body.data === null) {
    throw new Error(body.error?.message ?? "Request failed");
  }

  return body.data;
}

export default function ContextSlotSchemasAdminPage() {
  const [schemaKeys, setSchemaKeys] = useState<string[]>([]);
  const [selectedSchemaKey, setSelectedSchemaKey] = useState<string>("");
  const [schema, setSchema] = useState<ContextSlotSchemaResponse | null>(null);
  const [isLoadingKeys, setIsLoadingKeys] = useState(true);
  const [isLoadingSchema, setIsLoadingSchema] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  async function loadSchemaKeys() {
    setIsLoadingKeys(true);
    setErrorMessage(null);

    try {
      const data = await getApiData<SchemaKeysResponse>(
        "/api/v1/admin/context-slot-schemas",
      );
      setSchemaKeys(data.schemaKeys);
      setSelectedSchemaKey((current) => current || data.schemaKeys[0] || "");
    } catch (error) {
      setErrorMessage(toErrorMessage(error));
    } finally {
      setIsLoadingKeys(false);
    }
  }

  useEffect(() => {
    void loadSchemaKeys();
  }, []);

  useEffect(() => {
    if (!selectedSchemaKey) {
      setSchema(null);
      return;
    }

    let ignore = false;

    async function loadSchema() {
      setIsLoadingSchema(true);
      setErrorMessage(null);

      try {
        const data = await getApiData<ContextSlotSchemaResponse>(
          `/api/v1/admin/context-slot-schemas/${selectedSchemaKey}`,
        );

        if (!ignore) {
          setSchema(data);
        }
      } catch (error) {
        if (!ignore) {
          setSchema(null);
          setErrorMessage(toErrorMessage(error));
        }
      } finally {
        if (!ignore) {
          setIsLoadingSchema(false);
        }
      }
    }

    void loadSchema();

    return () => {
      ignore = true;
    };
  }, [selectedSchemaKey]);

  const sortedItems = useMemo(
    () => [...(schema?.items ?? [])].sort((a, b) => a.priority - b.priority),
    [schema],
  );

  return (
    <main className="h-screen overflow-y-auto bg-zinc-50 text-zinc-950">
      <div className="border-b border-zinc-200 bg-white">
        <div className="mx-auto flex max-w-7xl flex-col gap-5 px-6 py-6 md:flex-row md:items-end md:justify-between">
          <div>
            <div className="mb-3 flex items-center gap-2 text-sm font-medium text-zinc-500">
              <Database className="h-4 w-4" />
              Daily Rehearsal Admin
            </div>
            <h1 className="text-2xl font-semibold tracking-normal text-zinc-950">
              Context Slot Schemas
            </h1>
          </div>

          <div className="flex flex-col gap-2 md:min-w-80">
            <label className="text-sm font-medium text-zinc-700">
              Schema key
            </label>
            <div className="flex gap-2">
              <Select
                value={selectedSchemaKey}
                onValueChange={setSelectedSchemaKey}
                disabled={isLoadingKeys || schemaKeys.length === 0}
              >
                <SelectTrigger className="h-10 w-full bg-white">
                  <SelectValue
                    placeholder={isLoadingKeys ? "Loading..." : "No schema"}
                  />
                </SelectTrigger>
                <SelectContent>
                  {schemaKeys.map((schemaKey) => (
                    <SelectItem key={schemaKey} value={schemaKey}>
                      {schemaKey}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <button
                type="button"
                onClick={() => {
                  void loadSchemaKeys();
                }}
                className="inline-flex h-10 w-10 shrink-0 items-center justify-center rounded-md border border-zinc-300 bg-white text-zinc-700 hover:bg-zinc-100 disabled:cursor-not-allowed disabled:opacity-50"
                disabled={isLoadingKeys}
                aria-label="Refresh schema keys"
              >
                <RefreshCw
                  className={cn("h-4 w-4", isLoadingKeys && "animate-spin")}
                />
              </button>
            </div>
          </div>
        </div>
      </div>

      <div className="mx-auto flex max-w-7xl flex-col gap-6 px-6 py-6">
        {errorMessage && <StatusMessage kind="error" message={errorMessage} />}
        {!errorMessage && schemaKeys.length === 0 && !isLoadingKeys && (
          <StatusMessage kind="empty" message="No active schema keys found." />
        )}

        <SchemaSummary schema={schema} isLoading={isLoadingSchema} />

        <section className="overflow-hidden rounded-md border border-zinc-200 bg-white">
          <div className="flex items-center justify-between border-b border-zinc-200 px-4 py-3">
            <h2 className="text-sm font-semibold text-zinc-900">Slots</h2>
            <span className="text-sm text-zinc-500">
              {sortedItems.length} items
            </span>
          </div>
          <SlotTable items={sortedItems} isLoading={isLoadingSchema} />
        </section>

        {schema && (
          <details className="rounded-md border border-zinc-200 bg-white">
            <summary className="cursor-pointer px-4 py-3 text-sm font-semibold text-zinc-900">
              Raw response data
            </summary>
            <pre className="overflow-x-auto border-t border-zinc-200 bg-zinc-950 p-4 text-xs leading-5 text-zinc-100">
              {JSON.stringify(schema, null, 2)}
            </pre>
          </details>
        )}
      </div>
    </main>
  );
}

function SchemaSummary({
  schema,
  isLoading,
}: {
  schema: ContextSlotSchemaResponse | null;
  isLoading: boolean;
}) {
  const values = [
    ["Schema key", schema?.schemaKey ?? "-"],
    ["Name", schema?.name ?? "-"],
    ["Max follow-up", schema ? String(schema.maxFollowUpAttempt) : "-"],
    ["Active", schema ? String(schema.active) : "-"],
  ];

  return (
    <section className="grid gap-px overflow-hidden rounded-md border border-zinc-200 bg-zinc-200 md:grid-cols-4">
      {values.map(([label, value]) => (
        <div key={label} className="bg-white px-4 py-3">
          <p className="mb-1 text-xs font-medium uppercase tracking-normal text-zinc-500">
            {label}
          </p>
          <p className="min-h-6 truncate text-sm font-semibold text-zinc-950">
            {isLoading ? "Loading..." : value}
          </p>
        </div>
      ))}
    </section>
  );
}

function SlotTable({
  items,
  isLoading,
}: {
  items: ContextSlotSchemaItem[];
  isLoading: boolean;
}) {
  if (isLoading) {
    return <div className="p-6 text-sm text-zinc-500">Loading slots...</div>;
  }

  if (items.length === 0) {
    return <div className="p-6 text-sm text-zinc-500">No slots found.</div>;
  }

  return (
    <Table>
      <TableHeader>
        <TableRow className="bg-zinc-50 hover:bg-zinc-50">
          <TableHead className="w-20">Priority</TableHead>
          <TableHead className="w-32">Required</TableHead>
          <TableHead>Slot</TableHead>
          <TableHead className="w-36">Type</TableHead>
          <TableHead>Default</TableHead>
          <TableHead className="w-24">Active</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {items.map((item) => (
          <TableRow key={item.id}>
            <TableCell className="font-mono text-xs text-zinc-600">
              {item.priority}
            </TableCell>
            <TableCell>
              <RequiredBadge requiredLevel={item.requiredLevel} />
            </TableCell>
            <TableCell className="min-w-96 whitespace-normal py-4">
              <div className="mb-1 flex flex-wrap items-center gap-2">
                <span className="font-mono text-sm font-semibold text-zinc-950">
                  {item.slot.slotKey}
                </span>
                <span className="text-sm text-zinc-500">{item.slot.label}</span>
              </div>
              <p className="mb-2 text-sm leading-5 text-zinc-700">
                {item.slot.extractionHint || "-"}
              </p>
              {item.slot.followUpHint && (
                <p className="text-xs leading-5 text-zinc-500">
                  Follow-up: {item.slot.followUpHint}
                </p>
              )}
              {item.slot.options.length > 0 && (
                <div className="mt-3 flex flex-wrap gap-1.5">
                  {item.slot.options.map((option) => (
                    <Badge
                      key={option.id}
                      variant="outline"
                      className="border-zinc-300 bg-zinc-50 font-mono text-[11px] text-zinc-700"
                    >
                      {option.optionKey}
                    </Badge>
                  ))}
                </div>
              )}
            </TableCell>
            <TableCell>
              <Badge variant="secondary" className="font-mono text-xs">
                {item.slot.slotType}
              </Badge>
            </TableCell>
            <TableCell className="max-w-72 whitespace-normal text-sm text-zinc-700">
              {formatDefaultValue(item.slot)}
            </TableCell>
            <TableCell>
              {item.active ? (
                <CheckCircle2 className="h-4 w-4 text-emerald-600" />
              ) : (
                <AlertCircle className="h-4 w-4 text-zinc-400" />
              )}
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}

function RequiredBadge({ requiredLevel }: { requiredLevel: RequiredLevel }) {
  const className = {
    REQUIRED: "border-rose-200 bg-rose-50 text-rose-700",
    SOFT_REQUIRED: "border-amber-200 bg-amber-50 text-amber-700",
    OPTIONAL: "border-zinc-200 bg-zinc-50 text-zinc-600",
  }[requiredLevel];

  return (
    <Badge variant="outline" className={cn("font-mono text-[11px]", className)}>
      {requiredLevel}
    </Badge>
  );
}

function StatusMessage({
  kind,
  message,
}: {
  kind: "error" | "empty";
  message: string;
}) {
  return (
    <div
      className={cn(
        "flex items-center gap-2 rounded-md border px-4 py-3 text-sm",
        kind === "error"
          ? "border-rose-200 bg-rose-50 text-rose-800"
          : "border-zinc-200 bg-white text-zinc-600",
      )}
    >
      <AlertCircle className="h-4 w-4 shrink-0" />
      {message}
    </div>
  );
}

function formatDefaultValue(slot: ContextSlot) {
  if (slot.defaultOption) {
    return `${slot.defaultOption.optionKey} (${slot.defaultOption.label})`;
  }

  return slot.defaultLiteralValue || "-";
}

function toErrorMessage(error: unknown) {
  return error instanceof Error ? error.message : "Unexpected error";
}
