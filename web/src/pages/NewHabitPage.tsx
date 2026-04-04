import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router';
import { createHabit, fetchToday } from '../api/habits';
import { useUser } from '../auth/AuthContext';
import { HabitForm } from '../components/HabitForm';

export function NewHabitPage() {
  const user = useUser();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  // The API knows what "today" is in the user's time zone.
  const today = useQuery({ queryKey: ['today', 'current'], queryFn: () => fetchToday() });
  const create = useMutation({
    mutationFn: createHabit,
    onSuccess: (habit) => {
      void queryClient.invalidateQueries({ queryKey: ['habits'] });
      void queryClient.invalidateQueries({ queryKey: ['today'] });
      navigate(`/habits/${habit.id}`);
    },
  });

  if (today.isPending) return <div className="page-loading">Loading…</div>;

  return (
    <div className="page narrow">
      <h1>New habit</h1>
      <HabitForm
        today={today.data?.today ?? new Date().toISOString().slice(0, 10)}
        weekStart={user.weekStart}
        submitLabel="Create habit"
        onSubmit={(input) => create.mutateAsync(input)}
        onCancel={() => navigate(-1)}
      />
    </div>
  );
}
