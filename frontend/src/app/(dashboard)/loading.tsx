import { Skeleton } from "@/components/ui/skeleton"
import { PageHeader } from "@/components/common/page-header"

export default function DashboardLoading() {
  return (
    <div className="space-y-6">
      <div className="space-y-4">
        <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
           <div className="space-y-2">
             <Skeleton className="h-8 w-[200px]" />
             <Skeleton className="h-4 w-[300px]" />
           </div>
        </div>
        <Skeleton className="h-[1px] w-full" />
      </div>

       <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        {Array.from({ length: 4 }).map((_, i) => (
          <Skeleton key={i} className="h-[120px] rounded-xl" />
        ))}
      </div>
      
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-7">
        <Skeleton className="col-span-4 h-[400px] rounded-xl" />
        <Skeleton className="col-span-3 h-[400px] rounded-xl" />
      </div>
    </div>
  )
}
