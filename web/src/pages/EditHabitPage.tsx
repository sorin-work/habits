import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate, useParams } from 'react-router';
import { fetchHabit, updateHabit } from '../api/habits';
import type { HabitInput } from '../api/types';
import { useUser } from '../auth/AuthContext';
import { ErrorMessage } from '../components/ErrorMessage';
import { HabitForm } from '../components/HabitForm';

export function EditHabitPage() {
  const id = Number(useParams().id);
  const user = useUser();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const query = useQuery({ queryKey: ['habit', id], queryFn: () => fetchHabit(id) });
  const update = useMutation({
    mutationFn: (input: HabitInput) => updateHabit(id, input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['habit', id] });
      void queryClient.invalidateQueries({ queryKey: ['habits'] });
      void queryClient.invalidateQueries({ queryKey: ['today'] });
      navigate(`/habits/${id}`);
    },
  });

  if (query.isPending) return <div className="page-loading">Loading…</div>;
  if (query.isError) return <ErrorMessage error={query.error} />;

  return (
    <div className="page narrow">
      <h1>Edit habit</h1>
      <HabitForm
        habit={query.data.habit}
        today={new Date().toISOString().slice(0, 10)}
        weekStart={user.weekStart}
        submitLabel="Save changes"
        onSubmit={(input) => update.mutateAsync(input)}
        onCancel={() => navigate(`/habits/${id}`)}
      />
    </div>
  );
}
